package uk.co.sprily
package btf.web
package plugins

import java.net.InetAddress

import org.specs2.mutable.Specification

import play.api.test._
import play.api.test.Helpers._

import uk.co.sprily.dh.harvester.DeviceId
import uk.co.sprily.dh.modbus.ModbusDevice

class DeviceConfigSpec extends Specification {

  "DeviceConfig plugin" >> {
    "reads IP4 address" in new WithApplication(ipAddressConfig) {
      val genDevice = DeviceConfig.get.genDevice
      genDevice must_=== ModbusDevice(
        host = InetAddress.getByAddress(Array(192,168,1,5).map(_.toByte)),
        id   = DeviceId(1),
        port = 502,
        unit = 1)
    }

    "reads textual host name" in new WithApplication(hostnameConfig) {
      val genDevice = DeviceConfig.get.genDevice
      genDevice must_=== ModbusDevice(
        host = InetAddress.getByName(hostname),
        id   = DeviceId(1),
        port = 502,
        unit = 1)
    }

    "reads localhost" in new WithApplication(loopbackConfig) {
      val genDevice = DeviceConfig.get.genDevice
      genDevice must_=== ModbusDevice(
        host = InetAddress.getByName("localhost"),
        id   = DeviceId(1),
        port = 502,
        unit = 1)
    }
  }

  private lazy val ipAddressConfig = fakeApp.copy(additionalConfiguration = Map(
    "datahopper.generator-meter" -> Map(
      "host" -> "192.168.1.5",
      "id"   -> 1,
      "port" -> 502,
      "unit" -> 1)
    )
  )

  private lazy val hostnameConfig = fakeApp.copy(additionalConfiguration = Map(
    "datahopper.generator-meter" -> Map(
      "host" -> hostname,
      "id"   -> 1,
      "port" -> 502,
      "unit" -> 1)
    )
  )

  private lazy val loopbackConfig = fakeApp.copy(additionalConfiguration = Map(
    "datahopper.generator-meter" -> Map(
      "host" -> "localhost",
      "id"   -> 1,
      "port" -> 502,
      "unit" -> 1)
    )
  )

  private lazy val fakeApp = FakeApplication(withoutPlugins=List("uk.co.sprily.btf.web.plugins.Harvesting"))

  private def hostname = InetAddress.getLocalHost.getHostName
}
