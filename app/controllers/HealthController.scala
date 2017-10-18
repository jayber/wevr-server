package controllers

import javax.inject._

import play.api.mvc._

/**
  * Used
 */
@Singleton
class HealthController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def get() = Action { implicit request: Request[AnyContent] =>
    Ok("good")
  }
}
