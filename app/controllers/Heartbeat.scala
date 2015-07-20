package controllers

import akka.actor.{Actor, ActorRef, Props}
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.mvc._

/**
 * trydofor@moilioncircle.com 2015-07-20
 */

class Heartbeat extends Controller {

  def  heartbeat = Action { request =>
    val host = request.host
    val server = routes.Heartbeat.heartbeatServer
    Ok(views.html.heartbeat(host + server))
  }

  def heartbeatMaster = Action(parse.json) { implicit request =>
    val reqJson = request.body
    val resJson = Json.obj("result"->"success","request"->reqJson)
    Ok(resJson)
  }

  def heartbeatServer = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    val host = request.host
    val server = routes.Heartbeat.heartbeatMaster
    val url = "http://" + host + server
    TerminalActor.props(out, url)
  }


  object TerminalActor {
    def props(out: ActorRef, url: String) = Props(new TerminalActor(out, url))
  }

  class TerminalActor(out: ActorRef, url: String) extends Actor {
    val logger = Logger(TerminalActor.getClass)

    def receive = {
      case msg: JsValue =>
        logger.info(url)
        val request = WS.url(url)
          .withRequestTimeout(10000)
        val response = request.post(msg)

        response.map { r => out ! Json.parse(r.body) }
    }
  }
}
