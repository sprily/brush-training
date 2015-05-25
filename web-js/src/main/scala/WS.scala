package uk.co.sprily
package btf.webjs

import scala.concurrent.duration.FiniteDuration

import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.typedarray.ArrayBuffer

import monifu.reactive._
import monifu.reactive.channels._

import org.scalajs.dom

import upickle._

sealed trait WSState
case object WSOpen extends WSState
case object WSClosed extends WSState

trait WSModule {
  def connect(url: String, heartbeat: FiniteDuration): WS
}

trait WS {
  type Error = String
  def status: Observable[WSState]
  def data[T:Reader]: Observable[Either[Error,T]]
  def errors: Observable[dom.ErrorEvent]
  def disconnect(): Unit
}

object WSModule extends WSModule {

  import monifu.concurrent.Implicits.globalScheduler

  val logger = Logging.ajaxLogger(
    "WS",
    js.Dynamic.global.jsRoutes.uk.co.sprily.btf.web.controllers.Logging.log().absoluteURL().asInstanceOf[String])

  private[WSModule] case class IntervalHandler(value: Int) extends AnyVal

  /**
    * TODO: implement back-pressure, so that we receive the latest
    *       data when ready.  This requires an update to the WS controller
    *       server-side.  Currently PublishChannel is unbuffered.
    */
  protected[WSModule] class WSImpl(
      url: String,
      heartbeat: FiniteDuration,
      statusChannel: PublishChannel[WSState],
      dataChannel: PublishChannel[String],
      errorsChannel: PublishChannel[dom.ErrorEvent]) extends WS {

    private[this] var raw: Option[dom.WebSocket] = None
    private[this] var active = true
    private[this] var lastHB = Date.now()

    reconnect() // initial connection

    // Re-connect whenever the websocket disconnects
    status.filter(_ == WSClosed).foreach(_ => reconnect())
    status.filter(_ == WSOpen).foreach(_ => logger.info(s"Connected to $url"))

    // Set-up heartbeat
    dataChannel.filter(_ == "heartbeat").foreach(_ => lastHB = Date.now())
    private[this] val intervalH = IntervalHandler(dom.window.setInterval(checkHeartbeat _, heartbeat.toMillis))

    override def status = statusChannel
    override def errors = errorsChannel

    override def data[T:Reader] = {
      dataChannel.filter(_ != "heartbeat").map { s => try {
        Right(upickle.read[T](s))
      } catch {
        case e: Exception => Left(e.toString)
      }}
    }

    override def disconnect(): Unit = {
      logger.info(s"Closing websocket: $this")
      active = false
      dom.window.clearInterval(intervalH.value)
      raw.foreach(_.close())
    }

    private[this] def reconnect(): Unit = {
      if (active) {
        logger.info(s"Opening connection to $url")
        raw = Some {
          val ws = new dom.WebSocket(url)
          ws.onopen    = (e: dom.Event)        => statusChannel.pushNext(WSOpen)
          ws.onclose   = (e: dom.CloseEvent)   => statusChannel.pushNext(WSClosed)
          ws.onmessage = (e: dom.MessageEvent) => dataChannel.pushNext(e.data.asInstanceOf[String])
          ws.onerror   = (e: dom.ErrorEvent)   => errorsChannel.pushNext(e)
          ws
        }
      }
    }

    private[this] def checkHeartbeat(): Unit = {
      logger.debug(s"Checking heartbeat")
      if (lastHB + heartbeat.toMillis < Date.now()) {
        heartbeatFailed()
      }
    }

    private[this] def heartbeatFailed(): Unit = {
      logger.warning(s"Heartbeat failed")
      raw match {
        case Some(ws) if ws.readyState == dom.WebSocket.OPEN =>
          logger.info(s"Closing connection due to heartbeat failing")
          ws.close()
        case Some(_) =>
          logger.debug(s"Not closing connection due to heartbeat failure as it is not open")
        case None =>
          logger.error(s"Unexpected state: no raw websocket available")
          reconnect()
      }
    }

  }
    
  override def connect(url: String, heartbeat: FiniteDuration): WS = {

    val statusChannel = PublishChannel[WSState]()
    val dataChannel = PublishChannel[String]()
    val errorsChannel = PublishChannel[dom.ErrorEvent]()

    new WSImpl(url, heartbeat, statusChannel, dataChannel, errorsChannel)
  }


}
