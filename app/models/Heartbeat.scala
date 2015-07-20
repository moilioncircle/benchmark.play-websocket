package models

/**
 * trydofor@moilioncircle.com 2015-07-20
 */

import play.api.libs.json._

case class Heartbeat(id: String, status: String, gps: String)

object Heartbeat {
  implicit val fm = Json.format[Heartbeat]
}

