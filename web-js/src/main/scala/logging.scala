package uk.co.sprily
package btf.webjs

trait Logger {
  val debug: String => Unit
  val info: String => Unit
  val warning: String => Unit
  val error: String => Unit
}

/** Quite surprised I couldn't see anything built-in for this.
  *
  * Logging with this is as simple as it gets!
  */
object Logging {

  def logger(ns: String): Logger = new Logger {
    val debug   = log("debug") _
    val info    = log("info") _
    val warning = log("warning") _
    val error   = log("error") _

    private def log(level: String)(s: String) = {
      println(s"[$level] $ns : $s")
    }
  }

}
