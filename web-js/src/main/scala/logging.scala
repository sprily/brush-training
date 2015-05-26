package uk.co.sprily
package btf.webjs

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

trait Logger {
  def debug(msg: String): Unit
  def info(msg: String): Unit
  def warning(msg: String): Unit
  def error(msg: String): Unit
}

object Logging {

  private lazy val consoleAppender = new BrowserConsoleAppender()

  def logger(ns: String): Logger = {
    val underlying = Log4JavaScript.log4javascript.getLogger(ns)
    underlying.addAppender(consoleAppender)
    new LoggerImpl(underlying)
  }

  def ajaxLogger(ns: String, url: String): Logger = {
    val underlying = Log4JavaScript.log4javascript.getLogger(ns)
    underlying.addAppender(consoleAppender)
    underlying.addAppender(ajaxAppender(url))
    new LoggerImpl(underlying)
  }

  private def ajaxAppender(url: String) = {
    val a = new AjaxAppender(url)
    a.addHeader("Content-Type", "application/json")
    a.setLayout(new JsonLayout)
    a
  }

  private class LoggerImpl(underlying: Log4JSLogger) extends Logger {
    override def debug(msg: String) = underlying.debug(msg)
    override def info(msg: String) = underlying.info(msg)
    override def warning(msg: String) = underlying.warn(msg)
    override def error(msg: String) = underlying.error(msg)
  }

  object Log4JavaScript extends js.GlobalScope {
    val log4javascript: Log4JavaScript = js.native
  }

  /** Interface to log4javascript **/
  trait Log4JavaScript extends js.Object {
    def getLogger(name: String): Log4JSLogger = js.native
  }

  @JSName("log4javascript.Logger")
  trait Log4JSLogger extends js.Object {
    def debug(msg: String): Unit = js.native
    def info(msg: String): Unit = js.native
    def warn(msg: String): Unit = js.native
    def error(msg: String): Unit = js.native
    def addAppender(a: Log4JSAppender): Unit = js.native
  }


  @JSName("log4javascript.Appender")
  trait Log4JSAppender extends js.Object

  @JSName("log4javascript.BrowserConsoleAppender")
  class BrowserConsoleAppender extends Log4JSAppender

  @JSName("log4javascript.AjaxAppender")
  class AjaxAppender(url: String) extends Log4JSAppender {
    def setLayout(layout: Layout): Unit = js.native
    def addHeader(header: String, value: String): Unit = js.native
  }

  @JSName("log4javascript.Layout")
  trait Layout extends js.Object

  @JSName("log4javascript.JsonLayout")
  class JsonLayout extends Layout
}

