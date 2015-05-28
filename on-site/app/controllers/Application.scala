package uk.co.sprily
package btf.web
package controllers

import scala.concurrent.duration._

import akka.actor._

import play.api.mvc._
import play.api.Play.current

import scalaz.concurrent.Task
import scalaz.stream._

import upickle._

import uk.co.sprily.dh.harvester.DeviceId
import uk.co.sprily.dh.modbus.ModbusDevice
import uk.co.sprily.dh.modbus.ModbusResponse
import uk.co.sprily.dh.modbus.ModbusRequest

import btf.webshared._
import btf.web.plugins._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("It works"))
  }

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    MyWebSocketActor.props(
      out,
      (plugins.Harvesting.subscribe |> DeviceReadings.decode |> process1.stripNone),
      DeviceConfig.get
    )
  }

  object MyWebSocketActor {
    def props(out: ActorRef,
              stream: Process[Task,DeviceReadings],
              devices: DeviceConfig) = Props(
      new MyWebSocketActor(out, stream, devices)
    )
  }

  class MyWebSocketActor(
      out: ActorRef,
      stream: Process[Task,DeviceReadings],
      devices: DeviceConfig) extends Actor {

    type Output = Either[GridReadings,GeneratorReadings]

    import context._
    case object SendHeartbeat

    override def preStart() = {
      (stream observe (channel.lift(i => Task.delay(self ! i)))).run.runAsync(println)
      system.scheduler.scheduleOnce(3000.millis, self, SendHeartbeat)
    }

    def receive = {
      case SendHeartbeat =>
        out ! "heartbeat"
        system.scheduler.scheduleOnce(3000.millis, self, SendHeartbeat)

      case r@DeviceReadings(d,_,_,_,_,_,_) if d.id == devices.GridId =>
        val rs: Output = Left(GridReadings(current = r.current,
                              activePower = r.activePower,
                              reactivePower = r.reactivePower,
                              powerFactor = r.powerFactor,
                              voltage = r.voltage,
                              frequency = r.frequency))
        out ! upickle.write(rs)

      case r@DeviceReadings(d,_,_,_,_,_,_) if d.id == devices.GeneratorId =>
        val rs: Output = Right(GeneratorReadings(current = r.current,
                                   activePower = r.activePower,
                                   reactivePower = r.reactivePower,
                                   powerFactor = r.powerFactor,
                                   voltage = r.voltage,
                                   frequency = r.frequency))
        out ! upickle.write(rs)
    }
  }

}
