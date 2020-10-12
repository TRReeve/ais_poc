package ais_poc_etl.analytics
import akka.actor.ActorSystem

class GeoEnrichment(implicit system: ActorSystem) {



  //  Retrieve information from api/service

}

object GeoEnrichment {
  def apply(implicit system: ActorSystem): GeoEnrichment = new GeoEnrichment
}
