package uk.co.sprily
package btf.web
package controllers

import play.api._
import play.api.mvc._

object Router extends Controller {

  def javascriptRoutes = Action { implicit request =>
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.Application.socket,
        routes.javascript.Logging.log
      )
    ).as("text/javascript")
  }

}
