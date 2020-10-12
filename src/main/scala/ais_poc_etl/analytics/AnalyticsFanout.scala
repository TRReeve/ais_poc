package ais_poc_etl.analytics

import ais_poc_etl.validate.AisMessage
import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.alpakka.slick.javadsl.SlickSession
import akka.stream.alpakka.slick.scaladsl.Slick
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, _}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils

import scala.util.{Success, Try}

case class PreEnrich(latitude: Double,
                     longitude: Double,
                     mmsi: Long,
                     timestamp: Long)

case class EnrichedMessage(
                            name: String,
                            country_code: String,
                            admin_one: String,
                            admin_two: String,
                            latitude: Double,
                            longitude: Double,
                            timestamp: Long,
                            mmsi: Long
                          )

case class EnrichRequest(geodata: Seq[PreEnrich])

case class EnrichResponse(response: List[EnrichedMessage])

class AnalyticsFanout(implicit session: SlickSession) {

  import session.profile.api._


  def apiRequest(msg: EnrichRequest): Try[EnrichResponse] = {

    val timeout = 1800
    val requestConfig = RequestConfig.custom()
      .setConnectTimeout(timeout * 1000)
      .setConnectionRequestTimeout(timeout * 1000)
      .setSocketTimeout(timeout * 1000).build()
    val client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build()

    val post_request = new HttpPost("http://localhost:8000/geoenrich")
    post_request.addHeader("Content-Type", "application/json")
    post_request.setEntity(new StringEntity(msg.asJson.toString()))

    val response = client.execute(post_request).getEntity
    val to_string = EntityUtils.toString(response, "UTF-8")
    println(s"Enriched ${msg.geodata.size} messages")
    decode[EnrichResponse](to_string).toTry
  }

  val analytics_fanout: Flow[Seq[AisMessage], Seq[AisMessage], NotUsed] = Flow.fromGraph(GraphDSL.create() { implicit b =>

    import GraphDSL.Implicits._

    val broadcast = b.add(Broadcast[Seq[AisMessage]](3))

    val map_to_request: FlowShape[Seq[AisMessage], EnrichRequest] = b.add(Flow[Seq[AisMessage]].map { ais_messages =>
      EnrichRequest(
        ais_messages.map(x => PreEnrich(x.point.latitude, x.point.longitude, x.mmsi, x.timestamp)))
    })

    val enrich_data = b.add(Flow[EnrichRequest]
      .map(apiRequest)
      .collect { case Success(value) => value }
    )

    val sink_enrichments_to_db: FlowShape[EnrichResponse, Int] = b.add(Flow[EnrichResponse]
      .map(x => x.response)
      .mapConcat(identity)
      .async
      .via(Slick.flow(4, enriched_record =>
        sqlu"""INSERT INTO ais_etl.ais_message_geo_enrichment VALUES (${enriched_record.timestamp},
              ${enriched_record.mmsi},${enriched_record.country_code},
              ${enriched_record.admin_one},${enriched_record.admin_two},
              ${enriched_record.latitude}, ${enriched_record.longitude}, ${enriched_record.name})"""
      )))

    val sink_ais_messages_to_db: FlowShape[Seq[AisMessage], Int] = b.add(Flow[Seq[AisMessage]]
      .mapConcat(identity)
      .async
      .via(Slick.flow(4, ais_message =>
        sqlu"""INSERT INTO ais_etl.ais_message VALUES (${ais_message.timestamp},
              ${ais_message.mobile_type},${ais_message.mmsi},
              ${ais_message.point.latitude}, ${ais_message.point.longitude},
              ${ais_message.status}, ${ais_message.vessel_type},${ais_message.imo})"""
      )))

    broadcast.out(0) ~> map_to_request ~> enrich_data ~> sink_enrichments_to_db ~> Sink.ignore
    broadcast.out(1) ~> sink_ais_messages_to_db ~> Sink.ignore

    FlowShape(broadcast.in, broadcast.out(2))
  })

}
