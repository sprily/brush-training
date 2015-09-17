package uk.co.sprily
package btf.web
package plugins

import java.io.File
import java.net.InetAddress

import org.specs2.mutable.Specification

import play.api.test._
import play.api.test.Helpers._

import uk.co.sprily.dh.harvester.DeviceId
import uk.co.sprily.dh.modbus.ModbusDevice

//TODO : test writeable configuration
class DeviceConfigSpec extends Specification {

  "DeviceConfig plugin" >> {
    "reads IP4 address" in new WithApplication(ipAddressConfig) {
      val genDevice = DeviceConfig.get.snapshot.generator
      genDevice must_=== ModbusDevice(
        host = InetAddress.getByAddress(Array(192,168,1,5).map(_.toByte)),
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
      "unit" -> 1),
    "datahopper.device-config-file" -> tmpFile.getAbsolutePath
    )
  )

  private lazy val tmpFile = File.createTempFile("btf-testing", ".tmp.conf")

  private lazy val fakeApp = FakeApplication(
    withoutPlugins=List(
      "uk.co.sprily.btf.web.plugins.MqttPublisher",
      "uk.co.sprily.btf.web.plugins.Harvesting"))

  private def hostname = InetAddress.getLocalHost.getHostName
}
