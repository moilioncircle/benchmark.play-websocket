package controllers

import java.text.SimpleDateFormat
import java.util.Date

import play.api.libs.json._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * trydofor@moilioncircle.com 2015-07-20
 */

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def helloWorld = Action {
    // https://www.techempower.com/benchmarks/#section=data-r10&hw=ec2&test=db
    Ok(Json.obj("message" -> "Hello, World!"))
  }

  def datetime = Action {
    val date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val str = date.format(new Date)
    Ok( s"""{
           |"action":"Application.datetime",
           |"status":"ok",
           |"time":$str
           |}""".stripMargin)
  }

  def sleep(ms: Long) = Action.async {
    val futureJson = scala.concurrent.Future {
      Thread.sleep(ms)
      val date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      val str = date.format(new Date)
      str
    }

    futureJson.map(str => Ok(
      s"""{
         |"action":"Application.sleep",
         |"status":"ok",
         |"sleep":$ms,
         |"time":$str
         |}""".stripMargin
    ))
  }
}
