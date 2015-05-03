package uk.co.sprily
package btf.webjs

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom

object Test extends js.JSApp {

  val HelloMessage = ReactComponentB[String]("HelloMessage")
    .render(name => <.div("It works ", name))
    .build

  val mountNode = dom.document.getElementById("scalajsShoutOut")

  @JSExport
  def main(): Unit = {
    React.render(HelloMessage("too!"), mountNode)
  }

}
