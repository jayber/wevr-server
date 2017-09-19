package controllers

import play.api.Logger
import play.api.mvc.{Action, Controller}


class ClientLoggingController extends Controller {

  def log(userId: String, message: String) = Action {
    Logger("client").debug(s"userId: $userId, message: $message")
    Ok
  }

  def post(userId: String, message: String, stack: String) = Action {
    Logger("client").error(s"userId: $userId, message: $message\n$stack")
    Ok
  }
}
