package controllers

import play.api.Logger
import play.api.mvc.{Action, Controller}


class ClientLoggingController extends Controller {
  val xml = "<empty></empty>"

  def log(userId: String, message: String) = Action {
    Logger("client").debug(s"userId: $userId, message: $message")
    Ok(xml)
  }

  def post(user: String, message: String, stack: String) = Action {
    Logger("client").error(s"userId: $user, message: $message\n$stack")
    Ok(xml)
  }
}
