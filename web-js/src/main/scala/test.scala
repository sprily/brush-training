package uk.co.sprily
package btf.webjs

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.ScalazReact._
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom

object Test extends js.JSApp {

  class Backend($: BackendScope[Unit, State]) {

    import monifu.concurrent.Implicits.globalScheduler

    private[this] var ws: js.UndefOr[WSService.WSLike[String]] = js.undefined
    private[this] val wsURI = js.Dynamic.global.jsRoutes.uk.co.sprily.btf.web.controllers.Application.socket().webSocketURL()

    def start() = {
      ws = WSService.connect(wsURI.asInstanceOf[String])
      ws.get.data.foreach(update)
    }

    private def update(msg: String) = $.modState(s =>
      State(connected=true,
            latest=(msg +: s.latest).slice(0,10))
    )
  }

  case class State(connected: Boolean, latest: IndexedSeq[String])
  object State {
    def init = State(connected=false, latest=Vector.empty)
  }

  val Dashboard = ReactComponentB[Unit]("Dashboard")
    .initialState(State.init)
    .backend(new Backend(_))
    .renderS(($,_,S) => S.connected match {
      case false => <.p("Connecting to server...")
      case true  => <.div(S.latest.map(<.p(_)))
    })
    .componentDidMount(_.backend.start())
    .buildU


  //val Measurements = ReactComponentB[Measurement]("Measurements")
  //  .render(ms => {
  //    <.div(
  //      <.p(
  //        <.em("Current: "),
  //        ms.current),
  //      <.p(
  //        <.em("Voltage: "),
  //        ms.voltage)
  //      )
  //  })
  //  .build

  
  //val ST = ReactS.Fix[State]

  //def handleSubmit(e: ReactEventI) = (
  //  ST.retM(e.preventDefaultIO)
  //  >> ST.mod(s => State(s.t + 1, s.ms.copy(s.ms.current+10, s.ms.voltage+5))).liftIO
  //)

  //val App = ReactComponentB[Unit]("App")
  //  .initialState(State(0, Measurement(100, 200)))
  //  .renderS(($,_,S) =>
  //    <.div(
  //      <.h2("T" + S.t),
  //      Measurements(S.ms),
  //      <.form(^.onSubmit ~~> $._runState(handleSubmit))(
  //        <.button("Up!")
  //      )
  //    )
  //  ).buildU

  val mountNode = dom.document.getElementById("main-app")

  @JSExport
  def main(): Unit = {
    Dashboard() render mountNode
  }

}
