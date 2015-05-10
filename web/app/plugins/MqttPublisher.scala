package uk.co.sprily
package btf.web
package plugins

import scala.concurrent.duration._
import scala.concurrent.blocking
import scala.concurrent.Future

import com.github.kxbmap.configs._

import com.typesafe.scalalogging.LazyLogging

import play.api.Application
import play.api.Play
import play.api.Plugin

import scalaz.concurrent._
import scalaz.stream._

import uk.co.sprily.dh.modbus.ModbusResponse
import uk.co.sprily.mqtt._

class MqttPublisher(app: Application) extends Plugin
                                         with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  type MqttClient = AsyncSimpleClient.Client

  private var client: Option[MqttClient] = None
  private[this] lazy val options = loadOptions("datahopper.mqtt")

  lazy val root = app.configuration.underlying.get[String]("datahopper.mqtt.root")

  override def onStart() = {
    def initialConnect(attempts: Int): Future[MqttClient] = {
      AsyncSimpleClient.connect(options)
        .recoverWith {
          case (e: Exception) if attempts > 0 =>
            logger.error(s"Unable to connect to MQTT broker.  Re-trying. $e")
            Future { blocking { Thread.sleep(3000) } } flatMap (_ => initialConnect(attempts-1))
          case (e: Exception) =>
            logger.error(s"Unable to connect to MQTT broker.  Giving up. $e")
            Future.failed(e)
        }
    }

    logger.info("Attempting to connect to MQTT broker")
    initialConnect(5).onSuccess {
      case c =>
        logger.info(s"Successfully made initial connection to MQTT broker")
        client = Some(c)
    }
  }

  override def onStop() = {
    logger.info("Disconnecting from MQTT broker")
    client.foreach(AsyncSimpleClient.disconnect)
  }

  private[this] def loadOptions(key: String) = {
    logger.info(s"Attempting to load '$key' MQTT options")
    val cfg = app.configuration.underlying.getConfig(key)
    logger.info(s"'$key' raw config: $cfg")
    val o = MqttOptions(
      url = cfg.get[String]("url"),
      port = cfg.get[Int]("port"),
      clientId = ClientId.random(),
      cleanSession = true,
      username = Some(cfg.get[String]("username")),
      password = Some(cfg.get[String]("password")),
      keepAliveInterval = 60.seconds)
    val elided = o.copy(password=o.password.map(_ => "<elided>"))
    logger.info(s"Loaded '$key' MQTT options: ${elided}")
    o
  }

}

object MqttPublisher {

  lazy val publisher = Play.current.plugin[MqttPublisher]
    .getOrElse(throw new RuntimeException("MqttPublisher plugin not enabled"))

  def publish: Sink[Task,ModbusResponse] = {
    channel.lift[Task,ModbusResponse,Unit] { r =>
      Task {
        publisher.client.foreach { client =>
          AsyncSimpleClient.publish(
            client,
            Topic(s"${publisher.root}/${r.device.id.value}/data/raw"),
            r.measurement.toArray,
            AtMostOnce,
            retain=false
          )
        }
      }
    }
  }

}
