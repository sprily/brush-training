package uk.co.sprily
package btf.web
package plugins

import java.io.File
import java.io.FileWriter
import java.net.InetAddress

import com.github.kxbmap.configs._

import com.google.common.net.InetAddresses

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import play.api.Application
import play.api.Play
import play.api.Plugin

import scalaz.stream._

import uk.co.sprily.dh.harvester.DeviceId
import uk.co.sprily.dh.modbus.ModbusDevice
import uk.co.sprily.dh.modbus.ModbusNetLoc

case class Devices(
    generator: ModbusDevice,
    grid: ModbusDevice)

/**
  * Provides the device configuration.
  *
  * TODO make writing config to file optional
  */
class DeviceConfig(app: Application) extends Plugin
                                        with LazyLogging {

  val GeneratorId  = DeviceId(1)
  val GridId       = DeviceId(2)

  private[this] val deviceConfigFile: File = {
    val f = new File(app.configuration.
             getString("datahopper.device-config-file")
             getOrElse {
               throw new RuntimeException("Misconfigured - no 'datahopper.device-config-file' found'")
             }
    )

    val dir = f.getParentFile
    if (!dir.exists && !dir.mkdirs()) {
      throw new RuntimeException(s"Cannot create device config file: $f")
    }

    try {
      f.createNewFile()   // this checks for existence first
    } catch {
      case e: Exception =>
        throw new RuntimeException(s"Error setting up device config file: $f")
    }

    if (!f.canRead || !f.canWrite) {
      throw new RuntimeException(s"Cannot read and write device config file: $f")
    }

    f
  }

  private def writeDevices(devices: Devices): Unit = synchronized {
    val out = new FileWriter(deviceConfigFile)
    try {
      val cfg = s"""datahopper {
                   |  generator-meter {
                   |    host = "${devices.generator.host.getHostAddress}"
                   |    port = ${devices.generator.port},
                   |    unit = ${devices.generator.unit}
                   |  }
                   |  grid-meter {
                   |    host = "${devices.grid.host.getHostAddress}"
                   |    port = ${devices.grid.port},
                   |    unit = ${devices.grid.unit}
                   |  }
                   |}""".stripMargin
      out.write(cfg)
      out.flush()
      logger.info(s"Wrote new device config out to file")
    } catch {
      case e: Exception =>
        logger.error(s"Error writing config file: ${e.getMessage}")
        throw e
    } finally {
      out.close()
    }
  }

  private[this] lazy val gen  = async.signalOf[ModbusDevice](
    loadDeviceConfig("datahopper.generator-meter", as = GeneratorId))

  private[this] lazy val grid = async.signalOf[ModbusDevice](
    loadDeviceConfig("datahopper.grid-meter", as = GridId))

  def genSignal: async.immutable.Signal[ModbusDevice] = gen
  def gridSignal: async.immutable.Signal[ModbusDevice] = grid

  def set(ds: Devices): Devices = {
    gen.set(ds.generator).run
    grid.set(ds.grid).run
    writeDevices(ds)
    ds
  }

  def snapshot: Devices = {
    (genSignal.continuous zip gridSignal.continuous).take(1).runLog.run.headOption.map((Devices.apply _).tupled).get
  }

  private def loadDeviceConfig(key: String, as: DeviceId) = {
    logger.info(s"Attempting to load '$key' device.")
    try {
      val cfg = ConfigFactory.parseFile(deviceConfigFile)
      val fallback = app.configuration.underlying
      val merged = cfg.withFallback(fallback).getConfig(key)
      logger.info(s"'$key' raw config: $merged")
      val d = ModbusDevice(
        id   = as,
        host = merged.get[InetAddress]("host"),
        port = merged.get[Int]("port"),
        unit = merged.get[Int]("unit"))
      logger.info(s"Loaded '$key' device: $d")
      d
    } catch {
      case e: Exception =>
        logger.error(s"Error reading config file(s): ${e.getMessage}")
        throw e
    }
  }

  private implicit def inetAddressAtPath: AtPath[InetAddress] = Configs.atPath { (cfg, key) =>
    InetAddresses.forString(cfg.getString(key))
  }

}

object DeviceConfig {

  def get: DeviceConfig = Play.current.plugin[DeviceConfig]
    .getOrElse(throw new RuntimeException("DeviceConfig plugin not enabled"))

}

//object Test {
//
//  import scalaz.stream._
//  import uk.co.sprily.btf.web._
//  import uk.co.sprily.dh.modbus._
//  import uk.co.sprily.dh.harvester._
//  import java.util.concurrent.Executors
//  import java.net.InetAddress
//  import scala.concurrent.duration._
//
//  val device = ModbusDevice(DeviceId(1), InetAddress.getByName("192.168.1.200"), port=502, unit=5)
//  val req = ModbusRequest(device, RegRange(0xc550, 0xc587))
//  val handler = new ModbusRequestHandler(
//    ioPool = Executors.newFixedThreadPool(2), maxConnections=2, closeUnusedConnectionAfter = 1.minute)
//  def responses(n: Int) = handler.responses(reqs(n), 0.second)
//
//  def debug[A] = io.stdOutLines contramap ((a: A) => a.toString)
//  def reqs(n: Int) = Process.fill(n)(req)
//  
//}
