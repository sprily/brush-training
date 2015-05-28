package uk.co.sprily
package btf.web
package plugins

import java.net.InetAddress

import com.github.kxbmap.configs._

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
  */
class DeviceConfig(app: Application) extends Plugin
                                        with LazyLogging {

  val GeneratorId  = DeviceId(1)
  val GridId       = DeviceId(2)

  private[this] lazy val gen  = async.signalOf[ModbusDevice](
    loadDeviceConfig("datahopper.generator-meter", as = GeneratorId))

  private[this] lazy val grid = async.signalOf[ModbusDevice](
    loadDeviceConfig("datahopper.grid-meter", as = GridId))

  def genSignal: async.immutable.Signal[ModbusDevice] = gen
  def gridSignal: async.immutable.Signal[ModbusDevice] = grid

  def set(ds: Devices): Devices = {
    gen.set(ds.generator).run
    grid.set(ds.grid).run
    ds
  }

  def snapshot: Devices = {
    (genSignal.continuous zip gridSignal.continuous).take(1).runLog.run.headOption.map((Devices.apply _).tupled).get
  }

  private def loadDeviceConfig(key: String, as: DeviceId) = {
    logger.info(s"Attempting to load '$key' device.")
    val cfg = app.configuration.underlying.getConfig(key)
    logger.info(s"'$key' raw config: $cfg")
    val d = ModbusDevice(
      id   = as,
      host = cfg.get[InetAddress]("host"),
      port = cfg.get[Int]("port"),
      unit = cfg.get[Int]("unit"))
    logger.info(s"Loaded '$key' device: $d")
    d
  }

  private implicit def inetAddressAtPath: AtPath[InetAddress] = Configs.atPath { (cfg, key) =>
    InetAddress.getByName(cfg.getString(key))
  }

}

object DeviceConfig {

  def get: DeviceConfig = Play.current.plugin[DeviceConfig]
    .getOrElse(throw new RuntimeException("DeviceConfig plugin not enabled"))

    //Play.current.plugin[Harvesting]
    //  .getOrElse(throw new RuntimeException("Harvesting plugin not enabled"))
    //  .topic.subscribe
}
