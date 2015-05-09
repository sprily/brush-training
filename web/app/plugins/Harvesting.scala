package uk.co.sprily
package btf.web
package plugins

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors

import play.api.Application
import play.api.Play
import play.api.Plugin

import scalaz.concurrent._
import scalaz.stream._

class Harvesting(app: Application) extends Plugin {
  private val topic = async.topic[Int]()
  private[this] val running = new AtomicBoolean(false)

  override def onStart() = {

    // load the device config
    val deviceConfig = app.plugin[DeviceConfig]
      .getOrElse(throw new RuntimeException("Harvesting plugin depends on DeviceConfig"))

    running.set(true)
    val es = Executors.newFixedThreadPool(1)

    es.submit(
      new Runnable {
        private[this] var counter = 0
        override def run() = {
          while(running.get) {
            Thread.sleep(1000)
            counter += 1
            (Process.emit(counter) to topic.publish).run.run
          }
        }
      }
    )

  }

  override def onStop() = {
    running.set(false)
  }

}

object Harvesting {
  def subscribe: Process[Task,Int] = {
    Play.current.plugin[Harvesting]
      .getOrElse(throw new RuntimeException("Harvesting plugin not enabled"))
      .topic.subscribe
  }
}
