package controllers

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorRef, Props}
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.mvc._

import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * trydofor@moilioncircle.com 2015-07-20
 */

object Alipay extends Controller {

  val logger = Logger("debug")
  val defaultTimeout = 1000

  def alipay = Action { request =>
    val host = request.host
    val server = routes.Alipay.alipayServer
    val pay = routes.Alipay.alipayReq(0).url
    Ok(views.html.alipay(host + server, pay))
  }

  val counter = new AtomicInteger(0)

  val PayWaiting = 0
  val PaySuccess = 1
  val PayTimeout = 2
  val PayException = 3

  def status(st: Int) = st match {
    case PayWaiting => "waiting"
    case PaySuccess => "success"
    case PayTimeout => "timeout"
    case PayException => "exception"
  }

  def alipayReq(i: Int) = Action {
    counter.set(i)
    val tm = counter.get()
    val ok = status(tm)
    Ok( s"""<script>parent.log("result:$ok")</script>""").as(HTML)
  }

  def alipayAck = Action {
    val tm = counter.get()
    if (PayTimeout == tm) {
      Thread.sleep(defaultTimeout * 2)
      logger.info(s"simulate timeout")
    }

    Ok(
      s"""
        {
          "result":"${status(tm)}",
          "status":$tm
        }
      """)
  }

  def alipayServer = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    logger.info(s"get a alipay-websocket from ${request.remoteAddress}")
    val host = request.host
    val server = routes.Alipay.alipayAck
    val url = "http://" + host + server
    AlipayActor.props(out, url)
  }


  object AlipayActor {
    def props(out: ActorRef, url: String) = Props(new AlipayActor(out, url))
  }

  class AlipayActor(out: ActorRef, url: String) extends Actor {
    val interval = 1.second

    def receive = {
      case msg: JsValue =>
        val request = WS.url(url).withRequestTimeout(defaultTimeout)
        val response = request.get()
        response.onComplete {
          case Success(r) =>
            val rjson = Json.parse(r.body)
            val status = (rjson \ "status").as[Int]
            status match {
              case PaySuccess => out ! rjson
              case PayException => throw new IllegalStateException("simulate actor Exception")
              case _ =>
                out ! Json.obj("retry" -> rjson)
                context.system.scheduler.scheduleOnce(interval, self, msg)
            }
          case Failure(t) => // timeout exception
            out ! Json.obj("retry" -> ("got-exception=" + t.getMessage))
            context.system.scheduler.scheduleOnce(interval, self, msg)
        }
    }

    override def preStart(): Unit = {
      logger.info("pre-start")
    }

    override def postStop(): Unit = {
      logger.info("post-stop")
    }

    override def postRestart(reason: Throwable): Unit = {
      logger.info("post-restart")
      super.postRestart(reason)
    }
  }

}
