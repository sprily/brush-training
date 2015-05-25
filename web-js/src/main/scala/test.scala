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

import btf.webshared._

object Test extends js.JSApp with gauges {

  type Readings = Either[GridReadings, GeneratorReadings]

  class Backend($: BackendScope[Unit, State]) {

    import monifu.concurrent.Implicits.globalScheduler

    private[this] var ws: js.UndefOr[WS] = js.undefined
    private[this] val wsURI = js.Dynamic.global.jsRoutes.uk.co.sprily.btf.web.controllers.Application.socket().webSocketURL()

    def start() = {
      ws = WSModule.connect(wsURI.asInstanceOf[String], 10.seconds)
      ws.get.data[Readings].foreach {
        case Left(error) => println(error)
        case Right(r)    => update(r)
      }
      ws.get.status.foreach(onWSChange)
    }

    private def onWSChange(state: WSState) = $.modState { s =>
      s.copy(websocketState=state)
    }

    private def update(reading: Readings) = $.modState { s =>
      reading match {
        case Left(r@GridReadings(_,_,_,_,_,_)) => s.copy(
          grid = s.grid.copy(readings=r, lastUpdate=now()))
        case Right(r@GeneratorReadings(_,_,_,_,_,_)) => s.copy(
          generator = s.generator.copy(readings=r, lastUpdate=now()))
      }
    }

    private def now() = Some(new js.Date(js.Date.now()))

  }

  case class GaugePanel(
    label: String,
    current: Gauge, power: Gauge,
    mvars: Gauge,   pf: PFGauge.type,
    voltage: Gauge, frequency: Gauge)

  object GaugePanel {

    def grid      = GaugePanel("Grid Instrumentation",      current, power, mvars, pf, voltage, frequency)
    def generator = GaugePanel("Generator Instrumentation", current, power, mvars, pf, voltage, frequency)

    def current   = Gauge("Line Current", "A",      0, 120, minorTicks=3, scaleBy=0.001)
    def power     = Gauge("Active Power", "MW",     0, 5, majorTicks=4, minorTicks=9, scaleBy=0.00001)
    def mvars     = Gauge("Reactive Power", "MVAR",  -5, 5, majorTicks=9, minorTicks=4, scaleBy=0.00001)
    def pf        = PFGauge
    def voltage   = Gauge("Voltage", "kV",     0, 15, majorTicks=2, minorTicks=9, scaleBy=0.00001)
    def frequency = Gauge("Frequency", "Hz",    30, 70, majorTicks=3,minorTicks=9, scaleBy=0.01)

  }

  case class Instruments(grid: GaugePanel, generator: GaugePanel)

  object Instruments {
    def init = Instruments(GaugePanel.grid, GaugePanel.generator)
  }

  case class PanelState[+T <: PanelReadings](
      lastUpdate: Option[js.Date],
      readings: T) { // TODO option

    def lastUpdateStr = lastUpdate.map(_.toTimeString).getOrElse("Never") // todo locale
    def isActive = lastUpdate != None

  }

  object PanelState {
    def  init[T <: PanelReadings](init: T) = PanelState(
      lastUpdate = None,
      readings = init)
  }

  case class State(
      websocketState: WSState,
      instruments: Instruments,
      grid: PanelState[GridReadings],
      generator: PanelState[GeneratorReadings])

  object State {
    def init = State(
      websocketState=WSClosed,
      instruments=Instruments.init,
      grid=PanelState.init(GridReadings.init),
      generator=PanelState.init(GeneratorReadings.init))
  }

  val ActivePanel = ReactComponentB[(GaugePanel,PanelState[PanelReadings])]("Panel")
    .render { S => {
      val (panel, panelState) = S
      <.div(
        <.div(grid.row,
          <.div(grid.col(6), corner((panel.current,   panelState.readings.current))),
          <.div(grid.col(6), corner((panel.power,     panelState.readings.activePower)))
        ),
        <.div(grid.row,
          <.div(grid.col(6), corner((panel.mvars,     panelState.readings.reactivePower))),
          <.div(grid.col(6), powerFactor((panel.pf,   panelState.readings.powerFactor)))
        ),
        <.div(grid.row,
          <.div(grid.col(6), corner((panel.voltage,   panelState.readings.voltage))),
          <.div(grid.col(6), corner((panel.frequency, panelState.readings.frequency)))
        )
      )
    }}
    .build

  val InactivePanel = ReactComponentB[PanelState[PanelReadings]]("Panel")
    .render { S =>
      <.div(^.cls := "text-center",
        <.p("No Data Received")
      )
    }
    .build

  val Panel = ReactComponentB[(GaugePanel,PanelState[PanelReadings])]("Panel")
    .render { S =>
      <.div(^.cls := "panel panel-default",
        <.div(^.cls := "panel-heading text-center",
          <.p(^.cls := "panel-title", S._1.label)
        ),
        <.div(^.cls := "panel-body",
          if (S._2.isActive) ActivePanel(S) else InactivePanel(S._2)
        ),
        <.div(^.cls := "panel-footer text-center",
          <.small(<.em(s"Last Updated: ${S._2.lastUpdateStr}"))
        )
      )
    }
    .build

  val Dashboard = ReactComponentB[Unit]("Dashboard")
    .initialState(State.init)
    .backend(new Backend(_))
    .renderS(($,_,S) => S.websocketState match {
      case WSClosed => <.div(
        grid.row,
        <.p("Connecting to server...")
      )
      case WSOpen  => <.div(grid.row,

        <.div(
          grid.col(4),
          Panel((S.instruments.grid, S.grid))
        ),

        <.div(
          grid.col(4),
          <.img(^.src := "/assets/images/brush-logo.png"), ^.cls := "text-center"),

        <.div(
          grid.col(4),
          Panel((S.instruments.generator, S.generator))
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
