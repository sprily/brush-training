package uk.co.sprily
package btf.web
package controllers

import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("It works"))
  }

}
