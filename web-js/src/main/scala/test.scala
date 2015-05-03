package uk.co.sprily
package btf.webjs

import scala.scalajs.js
import org.scalajs.dom

object Test extends js.JSApp {
  def main(): Unit = {
    dom.document.getElementById("scalajsShoutOut").textContent = "It works too"
  }
}
