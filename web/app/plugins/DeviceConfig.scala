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

import uk.co.sprily.dh.harvester.DeviceId
import uk.co.sprily.dh.modbus.ModbusDevice
import uk.co.sprily.dh.modbus.ModbusNetLoc

/**
  * Provides the device configuration.
  */
class DeviceConfig(app: Application) extends Plugin
                                        with LazyLogging {

  val genDevice = loadDeviceConfig("datahopper.generator-meter")
  val gridDevice = loadDeviceConfig("datahopper.grid-meter")

  private def loadDeviceConfig(key: String) = {
    logger.info(s"Attempting to load '$key' device.")
    val cfg = app.configuration.underlying.getConfig(key)
    logger.info(s"'$key' raw config: $cfg")
    val d = ModbusDevice(
      id = DeviceId(cfg.get[Long]("id")),
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
