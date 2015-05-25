package uk.co.sprily
package btf.web
package controllers

import com.typesafe.scalalogging.LazyLogging

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

object Logging extends Controller with LazyLogging {

  def log = Action(BodyParsers.parse.json) { request =>
    request.body.validate[Seq[LogEntry]].fold(
      errors => {
        println(errors)
        println(request.body)
        BadRequest(s"Couldn't parse JSON body: $errors")
      },
      entries => { 
        entries.foreach { e => logger.info(e.message) }
        Ok("")  
      }
    )
  }

  private case class LogEntry(
      logger: String,
      timestamp: Long,
      level: String,
      url: String,
      message: String)

  private object LogEntry {
    implicit val reads: Reads[LogEntry] = (
      (JsPath \ "logger").read[String] and
      (JsPath \ "timestamp").read[Long] and
      (JsPath \ "level").read[String] and
      (JsPath \ "url").read[String] and
      (JsPath \ "message").read[String]
    )(LogEntry.apply _)
  }
}
