package uk.co.sprily
package btf.web
package controllers

import java.net.InetAddress
import java.net.UnknownHostException

import com.typesafe.scalalogging.LazyLogging

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.mvc._
import play.api.mvc.BodyParsers.parse

import uk.co.sprily.dh.modbus.ModbusDevice

import btf.web.plugins._

case class AppConfig(
    generator: MeterConfig,
    grid: MeterConfig)

case class MeterConfig(
    host: String,
    port: Int,
    unit: Int)

object Config extends Controller with LazyLogging {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def get = Action {
    val devices = DeviceConfig.get.snapshot
    Ok(views.html.config(
      appCfgForm.fill(toAppCfg(devices))
    ))
  }

  def update = Action { implicit request =>
    appCfgForm.bindFromRequest.fold(
      errs => BadRequest(views.html.config(errs)),
      cfg  => {
        val devices = DeviceConfig.get.snapshot
        DeviceConfig.get.set(applyCfg(devices, cfg))
        Redirect(routes.Application.index).flashing("success" -> "Configuration updated.")
      }
    )
  }

  // TODO: Use the Guava lib here
  val ipAddress: Constraint[String] = Constraint("constraints.ipaddress")(s =>
    try {
      logger.info(s"Validated ${InetAddress.getByName(s)}")
      Valid
    } catch {
      case (e: UnknownHostException) => Invalid(
        Seq(ValidationError(s"Invalid host")))
      case (e: Exception) => Invalid(
        Seq(ValidationError(s"Error parsing host: ${e.getMessage}")))
    }
  )

  val meterMapping = mapping(
    "host" -> nonEmptyText.verifying(ipAddress),
    "port" -> number(min = 1),
    "unit" -> number(min = 0, max = 255)
  )(MeterConfig.apply)(MeterConfig.unapply)

  val appCfgForm = Form(
    mapping(
      "generator" -> meterMapping,
      "grid"      -> meterMapping
    )(AppConfig.apply)(AppConfig.unapply)
  )

  private def toAppCfg(ds: Devices) = AppConfig(
    generator = toMeterCfg(ds.generator),
    grid      = toMeterCfg(ds.grid)
  )

  private def toMeterCfg(d: ModbusDevice) = MeterConfig(
    host = d.host.getHostName,
    port = d.port,
    unit = d.unit)

  private def applyCfg(d: ModbusDevice, cfg: MeterConfig): ModbusDevice = {
    d.copy(
      host = InetAddress.getByName(cfg.host),
      port = cfg.port,
      unit = cfg.unit
    )
  }

  private def applyCfg(ds: Devices, cfg: AppConfig): Devices = {
    ds.copy(
      generator = applyCfg(ds.generator, cfg.generator),
      grid      = applyCfg(ds.grid,      cfg.grid)
    )
  }

}
