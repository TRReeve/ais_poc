package ais_poc_etl.validate

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.alpakka.slick.javadsl.SlickSession
import akka.stream.alpakka.slick.scaladsl.Slick
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink}
import cats.data._
import cats.implicits._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.util.{Failure, Success, Try}

sealed trait ValidateData {
  /**
   * Handles validation of individual datapoints and chaining them
   * together with an Applicative to ensure we have a complete valid record
   * */

  type ValidationResult[A] = ValidatedNec[AisMessageValidationFailure, A]
  private val fmt = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss")

  def validateTimestamp(timestamp_string: String): ValidationResult[Long] = {

    Try(DateTime.parse(timestamp_string.trim, fmt).getMillis) match {
      case Failure(exception) => {
        AisMessageValidationFailure(exception.getMessage).invalidNec
      }
      case Success(value) => {
        value.validNec
      }
    }
  }

  def validateString(string: String): ValidationResult[String] = {

    Try(string) match {
      case Failure(exception) => AisMessageValidationFailure(exception.getMessage).invalidNec
      case Success(value) => {
        value.validNec
      }
    }
  }

  def validateLong(long: String): ValidationResult[Long] = {

    Try(long.toLong) match {
      case Failure(exception) => AisMessageValidationFailure(exception.getMessage).invalidNec
      case Success(value) => value.validNec
    }
  }

  def validateImoNumber(input: String): ValidationResult[Option[Long]] = {

    Try(input.toLong) match {
      case Failure(exception) => {
        if (input.trim == "Unknown") {
          None.validNec
        } else {
          AisMessageValidationFailure(exception.getMessage).invalidNec
        }
      }
      case Success(value) => Some(value).validNec
    }
  }

  def validatePoint(latitude: String, longitude: String): ValidationResult[Point] = {

    Try(Point(latitude.toDouble, longitude.toDouble)) match {
      case Failure(exception) => AisMessageValidationFailure(exception.getMessage).invalidNec
      case Success(value) => value.validNec
    }
  }

  def parseAisMessage(csv_row: String): ValidationResult[AisMessage] = {


    val split_to_array = csv_row.split(",")

    /**
     * Validate data in parallel to either Failed or Validated AIS Message.
     * */
    (
      validateTimestamp(split_to_array(0)), // timestamp
      validateString(split_to_array(1)), // mobile type
      validateLong(split_to_array(2)), //mmsi
      validatePoint(split_to_array(3), split_to_array(4)), //point
      validateString(split_to_array(5)), // status
      validateString(split_to_array(13)), // vessel type
      validateImoNumber(split_to_array(10))) // imo identifier
      .mapN(AisMessage) //If all validations successful then we get a fully formed AISMessage

  }
}

class AISValidation(implicit session: SlickSession) extends ValidateData {

  import session.profile.api._

  val validate_messages: Flow[String, AisMessage, NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>

      import GraphDSL.Implicits._

      val validate = b.add(Flow[String].map(parseAisMessage))

      val broadcast = b.add(Broadcast[ValidationResult[AisMessage]](2))

      val collect_failed_messages = b.add(Flow[ValidationResult[AisMessage]]
        .collect({ case Validated.Invalid(failure_message) => failure_message.toString })
        .via(Slick.flow(msg => sqlu"INSERT INTO ais_etl.failed_message (errors) VALUES (${msg})")))

      val collect_valid_messages = b.add(Flow[ValidationResult[AisMessage]]
        .collect({ case Validated.Valid(message) => message }))

      validate ~> broadcast ~> collect_failed_messages ~> Sink.ignore
      broadcast ~> collect_valid_messages

      FlowShape(validate.in, collect_valid_messages.out)

    })
}

object AISValidation {
  def apply(implicit session: SlickSession): AISValidation = new AISValidation()
}