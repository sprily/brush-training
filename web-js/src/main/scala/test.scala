package uk.co.sprily
package btf.webjs

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.ScalazReact._
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom

object Test extends js.JSApp {

  case class State(t: Int, ms: Measurement)
  case class Measurement(current: Long, voltage: Long)

  val Measurements = ReactComponentB[Measurement]("Measurements")
    .render(ms => {
      <.div(
        <.p(
          <.em("Current: "),
          ms.current),
        <.p(
          <.em("Voltage: "),
          ms.voltage)
        )
    })
    .build

  
  val ST = ReactS.Fix[State]

  def handleSubmit(e: ReactEventI) = (
    ST.retM(e.preventDefaultIO)
    >> ST.mod(s => State(s.t + 1, s.ms.copy(s.ms.current+10, s.ms.voltage+5))).liftIO
  )

  val App = ReactComponentB[Unit]("App")
    .initialState(State(0, Measurement(100, 200)))
    .renderS(($,_,S) =>
      <.div(
        <.h2("T" + S.t),
        Measurements(S.ms),
        <.form(^.onSubmit ~~> $._runState(handleSubmit))(
          <.button("Up!")
        )
      )
    ).buildU

  val mountNode = dom.document.getElementById("main-app")

  @JSExport
  def main(): Unit = {
    App() render mountNode
  }

}
