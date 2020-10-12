package ais_poc_etl.analytics


case class PreEnrichmentMessage(latitude: Double,
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

case class EnrichRequest(geodata: Seq[PreEnrichmentMessage])

case class EnrichResponse(response: List[EnrichedMessage])
