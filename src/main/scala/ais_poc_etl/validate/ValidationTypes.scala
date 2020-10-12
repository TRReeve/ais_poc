package ais_poc_etl.validate

case class Point(latitude: Double,
                 longitude: Double)

case class AisMessage(timestamp: Long,
                      mobile_type: String,
                      mmsi: Long,
                      point: Point,
                      status: String,
                      vessel_type: String,
                      imo: Option[Long])

case class AisMessageValidationFailure(error: String)
