package uk.co.sprily
package btf.webjs

import scala.concurrent.duration._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.ScalazReact._
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom

object Test extends js.JSApp with gauges {

  class Backend($: BackendScope[Unit, State]) {

    import monifu.concurrent.Implicits.globalScheduler

    private[this] var ws: js.UndefOr[WS[String]] = js.undefined
    private[this] val wsURI = js.Dynamic.global.jsRoutes.uk.co.sprily.btf.web.controllers.Application.socket().webSocketURL()

    def start() = {
      ws = WSModule.connect[String](wsURI.asInstanceOf[String], 10.seconds)
      ws.get.data.foreach(update)
    }

    private def update(msg: String) = $.modState(s =>
      State(connected=true,
            latest=(msg +: s.latest).slice(0,10),
            count = s.count+1)
    )
  }

  case class State(connected: Boolean, latest: IndexedSeq[String], count: Int) {
    def gaugeText = connected match {
      case false => "connecting"
      case true  => "connected"
    }
  }

  object State {
    def init = State(connected=false, latest=Vector.empty, count=0)
  }

  val Dashboard = ReactComponentB[Unit]("Dashboard")
    .initialState(State.init)
    .backend(new Backend(_))
    .renderS(($,_,S) => S.connected match {
      case false => <.div(
        <.p("Connecting to server..."),
        corner(Gauge(2,4,0.0, 6.0))
      )
      case true  => <.div(
        <.div(S.count),
        //corner(Gauge(S.count,4,0.0,S.count*20.0)),
        corner(Gauge(2,4,0.0, 6.0)),
        S.latest.map(<.p(_))
      )
    })
    .componentDidMount(_.backend.start())
    .buildU

  val mountNode = dom.document.getElementById("main-app")

  @JSExport
  def main(): Unit = {
    Dashboard() render mountNode
  }

}
