package uk.co.sprily
package btf.web
package controllers

import akka.actor._

import play.api.mvc._
import play.api.Play.current

import scalaz.concurrent.Task
import scalaz.stream._

import uk.co.sprily.dh.modbus.ModbusResponse

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("It works"))
  }

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    MyWebSocketActor.props(out, plugins.Harvesting.subscribe)
  }

  object MyWebSocketActor {
    def props(out: ActorRef, stream: Process[Task,ModbusResponse]) = Props(new MyWebSocketActor(out, stream))
  }

  class MyWebSocketActor(out: ActorRef, stream: Process[Task,ModbusResponse]) extends Actor {

    override def preStart() = {
      (stream observe (channel.lift(i => Task.delay(self ! i)))).run.runAsync(println)
    }

    def receive = {
      case ModbusResponse(device, ts, m) =>
        out ! s"$device : $ts"
    }
  }

}
