package uk.co.sprily
package btf.web
package plugins

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors
import scala.concurrent.duration._

import com.typesafe.scalalogging.LazyLogging

import play.api.Application
import play.api.Play
import play.api.Plugin

import scalaz._
import scalaz.concurrent._
import scalaz.stream._

import uk.co.sprily.dh.modbus.ModbusRequest
import uk.co.sprily.dh.modbus.ModbusResponse
import uk.co.sprily.dh.modbus.RegRange
import uk.co.sprily.dh.modbus.ModbusRequestHandler

class Harvesting(app: Application) extends Plugin
                                      with LazyLogging {

  lazy private val topic = async.topic[(ModbusResponse,ModbusRequest)]()

  lazy private val devices = DeviceConfig.get

  // Hard-coded reading requests because what we read is heavily tied
  // into the custom moniting application.
  lazy private val genRequests = List(
    ModbusRequest(devices.genDevice, RegRange(0xC550, 0xC587)) // table 1
  )

  lazy private val gridRequests = List(
    ModbusRequest(devices.gridDevice, RegRange(0xC550, 0xC587)) // table 1
  )

  lazy private val handler = new ModbusRequestHandler(
    ioPool = Executors.newFixedThreadPool(genRequests.length + gridRequests.length),
    maxConnections = 1,   // Leave connections available for other applications, Diris has limit of 4
    closeUnusedConnectionAfter = 1.minute)

  lazy private val killSwitch = async.signalOf[Boolean](false)

  lazy private val readings = merge.mergeN(
    Process.emitAll(genRequests ++ gridRequests).map { req =>
      handler.recurring(req, 1.second).zip(Process.constant(req))
    }
  )

  lazy private val killableReadings = (
    killSwitch.discrete.wye(readings)(wye.interrupt)
  )

  lazy val mqtt = MqttPublisher.publish contramap[(ModbusResponse,ModbusRequest)](_._1)

  override def onStart() = {

    val cleanup = (r: Throwable \/ Unit) =>
      r.fold(
        err     => { logger.error(s"Something went wrong: $err") ; sys.exit(-1) },
        success => logger.info(s"Shutdown")
      )

    (killableReadings observe (mqtt) to topic.publish).run.runAsync(cleanup)
  }

  override def onStop() = {
    killSwitch.set(true).run
    handler.shutdown()
  }

}

object Harvesting {
  def subscribe: Process[Task,(ModbusResponse,ModbusRequest)] = {
    Play.current.plugin[Harvesting]
      .getOrElse(throw new RuntimeException("Harvesting plugin not enabled"))
      .topic.subscribe
  }
}
