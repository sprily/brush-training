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
    label: String,
    current: Gauge, power: Gauge,
    mvars: Gauge,   pf: PFGauge.type,
    voltage: Gauge, frequency: Gauge)

  object GaugePanel {

    def grid      = GaugePanel("Grid Instrumentation",      current, power, mvars, pf, voltage, frequency)
    def generator = GaugePanel("Generator Instrumentation", current, power, mvars, pf, voltage, frequency)

    def current   = Gauge("Line Current", "A",      0, 120, minorTicks=3)
    def power     = Gauge("Active Power", "MW",     0, 5, majorTicks=4, minorTicks=9)   // TODO
    def mvars     = Gauge("Reactive Power", "MVAR",  -5, 5, majorTicks=9, minorTicks=4)
    def pf        = PFGauge
    def voltage   = Gauge("Voltage", "kV",     0, 15, majorTicks=2, minorTicks=9)  // TODO
    def frequency = Gauge("Frequency", "Hz",    30, 70, majorTicks=3,minorTicks=9)

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

  val Panel = ReactComponentB[(GaugePanel,Double)]("Panel")
    .render { S => {
      val (panel, value) = S
      <.div(
        <.div(grid.row,
          <.div(grid.col(12), <.p(BrushTheme.title, panel.label))
        ),
        <.div(grid.row,
          <.div(grid.col(6), corner((panel.current,   value))),
          <.div(grid.col(6), corner((panel.power,     value)))
        ),
        <.div(grid.row,
          <.div(grid.col(6), corner((panel.mvars,     value))),
          <.div(grid.col(6), powerFactor((panel.pf,   value)))
        ),
        <.div(grid.row,
          <.div(grid.col(6), corner((panel.voltage,   value))),
          <.div(grid.col(6), corner((panel.frequency, value)))
        )
      )
    }}
    .build

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
          Panel((S.instruments.grid, S.count))
        ),

        <.div(
          grid.col(4),
          <.img(^.src := "/assets/images/brush-logo.png"), ^.cls := "text-center"),

        <.div(
          grid.col(4),
          Panel((S.instruments.generator, S.count))
        )
      )
    })
    .componentDidMount(_.backend.start())
    .buildU

  val mountNode = dom.document.getElementById("main-app")

  @JSExport
  def main(): Unit = {
    GlobalStyles.addToDocument()
    BrushTheme.addToDocument()
    Dashboard() render mountNode
  }

}
