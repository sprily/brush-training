package uk.co.sprily
package btf.webjs

import scala.scalajs.js

import monifu.reactive._
import monifu.reactive.channels._

import org.scalajs.dom

sealed trait WSState
case object WSOpen extends WSState
case object WSClosed extends WSState

trait WSModule {
  def connect[T](url: String): WS[T]
}

trait WS[T] {
  def status: Observable[WSState]
  def data: Observable[String]    // TODO: make this generic
  def errors: Observable[dom.ErrorEvent]
  def disconnect(): Unit
}

object WSModule extends WSModule {

  import monifu.concurrent.Implicits.globalScheduler

  val logger = Logging.logger("WS")

  /**
    * TODO: implement back-pressure, so that we receive the latest
    *       data when ready.  This requires an update to the WS controller
    *       server-side.  Currently PublishChannel is unbuffered.
    */
  protected[WSModule] class WSImpl[T](
      url: String,
      statusChannel: PublishChannel[WSState],
      dataChannel: PublishChannel[String],
      errorsChannel: PublishChannel[dom.ErrorEvent]) extends WS[T] {

    private[this] var raw: Option[dom.WebSocket] = None// = new dom.WebSocket(url)
    private[this] var active = true

    reconnect() // initial connection

    override def status = statusChannel
    override def data   = dataChannel
    override def errors = errorsChannel

    // Re-connect whenever the websocket disconnects
    status.filter(_ == WSClosed).foreach(_ => reconnect())
    status.filter(_ == WSOpen).foreach(_ => logger.info(s"Connected to $url"))

    override def disconnect(): Unit = {
      logger.info(s"Closing websocket: $this")
      active = false
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
  }
    
  override def connect[T](url: String): WS[T] = {

    val statusChannel = PublishChannel[WSState]()
    val dataChannel = PublishChannel[String]()
    val errorsChannel = PublishChannel[dom.ErrorEvent]()

    new WSImpl(url, statusChannel, dataChannel, errorsChannel)
  }


}
