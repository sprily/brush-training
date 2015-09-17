package uk.co.sprily
package btf.web

import scodec._
import scodec.bits._
import codecs._

import scalaz.stream._

import uk.co.sprily.dh.modbus.ModbusDevice
import uk.co.sprily.dh.modbus.ModbusResponse
import uk.co.sprily.dh.modbus.ModbusRequest

import btf.webshared._

object DeviceReadings {

  def decode: Process1[(ModbusRequest,ModbusResponse), Option[DeviceReadings]] = {
    process1.lift {
      case (ModbusRequest(_,selection), ModbusResponse(device,ts,data)) =>
        registers.decode(data.toBitVector).fold(
          err => None,
          rs  => Some {
            val readings: Map[Int,Int] = (selection.start to selection.end by 2).zip(rs.value).toMap

            // TODO: following could fail
            DeviceReadings(
              device = device,
              current = readings(0xC560),
              activePower = readings(0xC568),
              reactivePower = readings(0xC56A),
              powerFactor = readings(0xC56E),
              voltage = readings(0xC556),
              frequency = readings(0xC55E))
          }
        )
    }
  }

  private val register = int32
  private val registers = vector(register)

}

case class DeviceReadings(
    device: ModbusDevice,
    current: Int,
    activePower: Int,
    reactivePower: Int,
    powerFactor: Int,
    voltage: Int,
    frequency: Int) extends PanelReadings
