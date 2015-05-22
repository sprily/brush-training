package uk.co.sprily
package btf.webjs

import scala.concurrent.duration._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.ScalazReact._
import scalacss.Defaults._
import scalacss.ScalaCssReact._
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

    def current   = Gauge("A",      0, 120)
    def power     = Gauge("MW",     0, 5)   // TODO
    def mvars     = Gauge("MVAr",  -5, 5, majorTicks=4)
    def pf        = Gauge("cos φ", -1, 1, majorTicks=2, precision=2)  // TODO
    def voltage   = Gauge("kV",     0, 15)  // TODO
    def frequency = Gauge("Hz",    30, 70, majorTicks=1)

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
        grid.row,
        <.p("Connecting to server...")
      )
      case true  => <.div(grid.row,

        <.div(
          grid.col(4),
          <.div(grid.row,
            <.div(grid.col(6), corner((S.instruments.grid.current,   S.count))),
            <.div(grid.col(6), corner((S.instruments.grid.power,     S.count)))
          ),
          <.div(grid.row,
            <.div(grid.col(6), corner((S.instruments.grid.mvars,     S.count))),
            <.div(grid.col(6), corner((S.instruments.grid.pf,        S.count)))
          ),
          <.div(grid.row,
            <.div(grid.col(6), corner((S.instruments.grid.voltage,   S.count))),
            <.div(grid.col(6), corner((S.instruments.grid.frequency, S.count)))
          )
        ),

        <.div(grid.col(4)),

        <.div(
          grid.col(4),
          <.div(grid.row,
            <.div(grid.col(6), corner((S.instruments.generator.current,   S.count))),
            <.div(grid.col(6), corner((S.instruments.generator.power,     S.count)))
          ),
          <.div(grid.row,
            <.div(grid.col(6), corner((S.instruments.generator.mvars,     S.count))),
            <.div(grid.col(6), corner((S.instruments.generator.pf,        S.count)))
          ),
          <.div(grid.row,
            <.div(grid.col(6), corner((S.instruments.generator.voltage,   S.count))),
            <.div(grid.col(6), corner((S.instruments.generator.frequency, S.count)))
          )
        )
      )
    })
    .componentDidMount(_.backend.start())
    .buildU

  val mountNode = dom.document.getElementById("main-app")

  @JSExport
  def main(): Unit = {
    GlobalStyles.addToDocument()
    Dashboard() render mountNode
  }

}
