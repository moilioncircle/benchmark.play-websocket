package controllers

import akka.actor.{Actor, ActorRef, Props}
import play.api.Logger
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.mvc._

/**
 * trydofor@moilioncircle.com 2015-07-20
 */

object Heartbeat extends Controller {

  val logger = Logger("debug")

  def heartbeat = Action { request =>
    val host = request.host
    val server = routes.Heartbeat.heartbeatServer
    Ok(views.html.heartbeat(host + server))
  }

  def heartbeatMaster = Action(parse.json) { implicit request =>
    val reqJson = request.body
    val resJson = Json.obj("result" -> "success", "request" -> reqJson)
    Ok(resJson)
  }

  def command(id: String) = Action {
    val out = Cache.getAs[ActorRef](id)
    logger.info(s"send command to terminal id=$id ,by actior=$out")
    out.foreach { a =>
      a ! Json.obj("id" -> id, "command" -> "a test command to you")
    }
    Ok(s"command to id=$id  via=$out")
  }

  def heartbeatServer = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    logger.info(s"get a heartbeat-websocket from ${request.remoteAddress}")
    val host = request.host
    val server = routes.Heartbeat.heartbeatMaster
    val url = "http://" + host + server
    TerminalActor.props(out, url)
  }


  object TerminalActor {
    def props(out: ActorRef, url: String) = Props(new TerminalActor(out, url))
  }

  class TerminalActor(out: ActorRef, url: String) extends Actor {

    def receive = {
      case msg: JsValue =>
        logger.info(url)
        val id = (msg \ "id").as[String]
        Cache.set(id, out, 5 * 60 * 1000)
        val request = WS.url(url)
          .withRequestTimeout(10000)
        val response = request.post(msg)

        response.map { r => out ! Json.parse(r.body) }
    }
  }
}
