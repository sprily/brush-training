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

    private[this] var staleTimer: js.UndefOr[js.timers.SetIntervalHandle] = js.undefined
    private[this] var ws: js.UndefOr[WS] = js.undefined
    private[this] val wsURI = js.Dynamic.global.jsRoutes.uk.co.sprily.btf.web.controllers.Application.socket().webSocketURL()

    private[this] val logger = Logging.ajaxLogger(
      "uk.co.sprily.btf.webjs.Main.Backend",
      js.Dynamic.global.jsRoutes.uk.co.sprily.btf.web.controllers.Logging.log().absoluteURL().asInstanceOf[String])

    val configHref = js.Dynamic.global.jsRoutes.uk.co.sprily.btf.web.controllers.Config.get().absoluteURL().asInstanceOf[String]

    def start() = {
      logger.info("Starting Backend")
      ws = WSModule.connect(wsURI.asInstanceOf[String], 10.seconds)
      ws.get.data[Readings].foreach {
        case Left(error) => logger.error(s"Error getting Readings from websocket: $error")
        case Right(r)    => update(r)
      }
      ws.get.status.foreach(onWSChange)

      staleTimer = js.timers.setInterval(1000)(checkStale())
    }

    def stop() = {
      logger.info("Stopping Backend")
      ws foreach(_.disconnect())
      staleTimer foreach js.timers.clearInterval
    }

    private def checkStale() = $.modState(s => s)

    private def onWSChange(state: WSState) = $.modState { s =>
      logger.info(s"WS state changed to: $state")
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

    def grid      = GaugePanel("Grid Instrumentation / 电网仪表",      current, power, mvars, pf, voltage, frequency)
    def generator = GaugePanel("Generator Instrumentation / 发电机仪表", current, power, mvars, pf, voltage, frequency)

    def current   = Gauge("Line Current / 线路电路", "A",      0, 300, minorTicks=4, scaleBy=0.001)
    def power     = Gauge("Active Power / 有功功率", "MW",     -5, 5, majorTicks=9, minorTicks=4, scaleBy=0.00001)
    def mvars     = Gauge("Reactive Power / 无功功率", "MVAR",  -5, 5, majorTicks=9, minorTicks=4, scaleBy=0.00001)
    def pf        = PFGauge
    def voltage   = Gauge("Voltage / 电压", "kV",     0, 15, majorTicks=2, minorTicks=9, scaleBy=0.00001)
    def frequency = Gauge("Frequency / 频率", "Hz",    45, 55, majorTicks=9,minorTicks=3, scaleBy=0.01)

  }

  case class Instruments(grid: GaugePanel, generator: GaugePanel)

  object Instruments {
    def init = Instruments(GaugePanel.grid, GaugePanel.generator)
  }

  case class PanelState[+T <: PanelReadings](
      lastUpdate: Option[js.Date],
      readings: T) { // TODO option

    val staleTimout = 5000 //ms

    def lastUpdateStr = lastUpdate.map(_.toTimeString).getOrElse("Never") // todo locale

    // whether the data stream has bee activated, ie - we've seen some data
    def isActive = lastUpdate != None

    // whether the last data seen is now too old to display.
    // if we're not active, then we're not stale.
    def isStale = lastUpdate.map(_.getTime + staleTimout < js.Date.now()).getOrElse(false)

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

  val StalePanel = ReactComponentB[PanelState[PanelReadings]]("Panel")
    .render ( panelState =>
      <.div(^.cls := "text-center",
        <.p(s"No data received since ${panelState.lastUpdateStr}")
        )
    )
    .build

  val Panel = ReactComponentB[(GaugePanel,PanelState[PanelReadings])]("Panel")
    .render { S =>
      <.div(^.cls := "panel panel-default",
        <.div(^.cls := "panel-heading text-center",
          <.p(^.cls := "panel-title", S._1.label)
        ),
        <.div(^.cls := "panel-body",
          (S._2.isActive, S._2.isStale) match {
            case (false, _)    => InactivePanel(S._2)
            case (true, false) => ActivePanel(S)
            case (true, true)  => StalePanel(S._2)
          }
        ),
        <.div(^.cls := "panel-footer text-center",
          <.small(<.em(s"Last Updated: ${S._2.lastUpdateStr}"))
        )
      )
    }
    .build

  val Logo = ReactComponentB[Unit]("Logo")
    .render { _ =>
      <.div(grid.row,
        <.div(grid.col(12),
          <.img(^.src := "/assets/images/brush-logo.png")
        )
      )
    }
    .buildU

  val EstablishingConnection = ReactComponentB[Unit]("Establishing")
    .render { _ =>
      <.div(
        <.div(grid.row,
          <.div(grid.col(12),
            <.h1(
              ^.cls := "animate",
              "Establishing Connection")   // TODO localise
          )
        )
      )
    }.buildU

  val NoDataStream = ReactComponentB[(State,Backend)]("Panels")
    .render ( S => {
      val (state, backend) = S
      <.div(grid.row,
        <.div(grid.col(4)),   // empty
        <.div(grid.col(4), ^.cls := "text-center",
          Logo(),
          <.a(
            ^.href := backend.configHref,
            "Configuration"),
          EstablishingConnection()
        ),
        <.div(grid.col(4))    // empty
      )
    })
    .build

  val DataStream = ReactComponentB[(State,Backend)]("Panels")
    .render ( S => {
      val (state, backend) = S
      <.div(grid.row,
        <.div(
          grid.col(4),
          Panel((state.instruments.grid, state.grid))
        ),
        <.div(grid.col(4), ^.cls := "text-center",
          Logo(),
          <.a(
            ^.href := backend.configHref,
            "Configuration")
        ),
        <.div(
          grid.col(4),
          Panel((state.instruments.generator, state.generator))
        )
      )
    })
    .build

  val Dashboard = ReactComponentB[Unit]("Dashboard")
    .initialState(State.init)
    .backend(new Backend(_))
    .render((P,S,B) => S.websocketState match {
      case WSClosing => NoDataStream((S,B))
      case WSClosed  => NoDataStream((S,B))
      case WSOpen    => DataStream((S,B))
    })
    .componentDidMount(_.backend.start())
    .componentWillUnmount(_.backend.stop())
    .buildU

  val mountNode = dom.document.getElementById("main-app")

  @JSExport
  def main(): Unit = {
    GlobalStyles.addToDocument()
    BrushTheme.addToDocument()
    Dashboard() render mountNode
  }

}
