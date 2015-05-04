package uk.co.sprily
package btf.webjs

import scala.scalajs.js

import monifu.reactive._
import monifu.reactive.channels._

import org.scalajs.dom

sealed trait WSState
case object WSOpen extends WSState
case object WSClosed extends WSState

object WSService {

  import monifu.concurrent.Implicits.globalScheduler

  trait WSLike[T] {
    def status: Observable[WSState]
    def data: Observable[T]
    def errors: Observable[dom.ErrorEvent]
  }

  /**
    * TODO: implement back-pressure, so that we receive the latest
    *       data when ready.  This requires an update to the WS controller
    *       server-side.  Currently PublishChannel is unbuffered.
    */
  protected[WSService] case class WS[T](
      rawWS: dom.WebSocket,
      statusChannel: PublishChannel[WSState],
      dataChannel: PublishChannel[T],
      errorsChannel: PublishChannel[dom.ErrorEvent]) extends WSLike[T] {

    override def status = statusChannel
    override def data   = dataChannel
    override def errors = errorsChannel
  }
    
  def connect(url: String): WSLike[String] = {
    val rawWS = new dom.WebSocket(url)
    val statusChannel = PublishChannel[WSState]()
    val dataChannel = PublishChannel[String]()
    val errorsChannel = PublishChannel[dom.ErrorEvent]()

    rawWS.onopen    = (e: dom.Event) => statusChannel.pushNext(WSOpen)
    rawWS.onclose   = (e: dom.CloseEvent) => statusChannel.pushNext(WSClosed)
    rawWS.onmessage = (e: dom.MessageEvent) => dataChannel.pushNext(e.data.asInstanceOf[String])

    WS(rawWS, statusChannel, dataChannel, errorsChannel)
  }

}
