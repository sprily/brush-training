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
      s.copy(connected=true,
             latest=(msg +: s.latest).slice(0,10),
             count = s.count+1)
    )
  }

  case class GaugePanel(
    current: Gauge, power: Gauge,
    mvars: Gauge,   pf: Gauge,
    voltage: Gauge, frequency: Gauge)

  object GaugePanel {

    def grid      = GaugePanel(current, power, mvars, pf, voltage, frequency)
    def generator = GaugePanel(current, power, mvars, pf, voltage, frequency)

    def current   = std("A",     0, 120)
    def power     = std("kW",    0, 40)
    def mvars     = std("kVAr",  0, 40)
    def pf        = std("",      0, 1)
    def voltage   = std("V",     0, 500)
    def frequency = std("Hz",   30, 70)

    private def std(label: String, min: Double, max: Double, scaleBy: Double = 1.0) = Gauge(
      GaugeLayout(2, 4, min, max),
      DataConfig(label, scaleBy))
  }

  case class Instruments(grid: GaugePanel, generator: GaugePanel)

  case class State(connected: Boolean, latest: IndexedSeq[String], count: Int, instruments: Instruments) {
    def gaugeText = connected match {
      case false => "connecting"
      case true  => "connected"
    }
  }
  object Instruments {
    def init = Instruments(GaugePanel.grid, GaugePanel.generator)
  }

  object State {
    def init = State(connected=false, latest=Vector.empty, count=0, instruments=Instruments.init)
  }

  val Dashboard = ReactComponentB[Unit]("Dashboard")
    .initialState(State.init)
    .backend(new Backend(_))
    .renderS(($,_,S) => S.connected match {
      case false => <.div(
        <.p("Connecting to server..."),
        corner((S.instruments.grid.current, 0.0)),
        corner((S.instruments.grid.power, 0.0)),
        corner((S.instruments.grid.mvars, 0.0)),
        corner((S.instruments.grid.pf, 0.0)),
        corner((S.instruments.grid.voltage, 0.0)),
        corner((S.instruments.grid.frequency, 0.0))
      )
      case true  => <.div(
        <.div(S.count),
        corner((S.instruments.grid.current, S.count)),
        corner((S.instruments.grid.power, S.count)),
        corner((S.instruments.grid.mvars, S.count)),
        corner((S.instruments.grid.pf, S.count)),
        corner((S.instruments.grid.voltage, S.count)),
        corner((S.instruments.grid.frequency, S.count)),
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
