package uk.co.sprily
package btf.web
package controllers

import com.typesafe.scalalogging.LazyLogging
import com.typesafe.scalalogging.Logger

import org.slf4j.LoggerFactory

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import dh.util.RecentLogs

object Logging extends Controller with LazyLogging {

  def log = Action(BodyParsers.parse.json) { request =>
    request.body.validate[Seq[LogEntry]].fold(
      errors => {
        logger.error(s"Couldn't parse JSON body: $errors")
        BadRequest(s"Couldn't parse JSON body: $errors")
      },
      entries => { 
        entries.foreach(logEntry)
        Ok("")  
      }
    )
  }

  def recent = Action { request =>
    val entries = RecentLogs.latest.take(1).runLastOr(Seq.empty[String]).run
    Ok(views.html.logs(entries))
  }

  private def logEntry(e: LogEntry): Unit = {
    val clientLogger = Logger(LoggerFactory.getLogger(e.logger))
    e.level.toLowerCase match {
      case "debug"   => clientLogger.debug(e.message)
      case "info"    => clientLogger.info(e.message)
      case "warning" => clientLogger.warn(e.message)
      case "warn"    => clientLogger.warn(e.message)
      case "error"   => clientLogger.error(e.message)
      case other     => clientLogger.error(e.message)
    }
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

