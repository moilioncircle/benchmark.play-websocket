package controllers

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorRef, Props}
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration._

/**
 * trydofor@moilioncircle.com 2015-07-20
 */

object Alipay extends Controller {

  def alipay = Action { request =>
    val host = request.host
    val server = routes.Alipay.alipayServer
    val pay = routes.Alipay.alipayReq.url
    Ok(views.html.alipay(host + server, pay))
  }

  val counter = new AtomicInteger(1)

  def alipayReq = Action {
    val next = counter.addAndGet(1)
    Ok(s"""<script>parent.log("pay-times:$next,result:${next % 2 == 0}")</script>""").as(HTML)
  }

  def alipayAck = Action {
    val tm = counter.get()
    val ok = tm % 2 == 0
    Ok(
      s"""
        {
          "success":$ok,
          "times":$tm
        }
      """)
  }

  def alipayServer = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    val host = request.host
    val server = routes.Alipay.alipayAck
    val url = "http://" + host + server
    AlipayActor.props(out, url)
  }


  object AlipayActor {
    def props(out: ActorRef, url: String) = Props(new AlipayActor(out, url))
  }

  class AlipayActor(out: ActorRef, url: String) extends Actor {
    val logger = Logger(AlipayActor.getClass)
    val interval = 1.second

    def receive = {
      case msg: JsValue =>
        logger.info(url)
        val request = WS.url(url).withRequestTimeout(10000)
        val response = request.get()
        response.map { r =>
          val rjson = Json.parse(r.body)
          val success = (rjson \ "success").get.as[Boolean]
          if (success) {
            out ! rjson
          } else {
            out ! Json.obj("retry" -> ("retry at ms=" + System.currentTimeMillis()))
            context.system.scheduler.scheduleOnce(interval, self, msg)
          }
        }
    }
  }

}
