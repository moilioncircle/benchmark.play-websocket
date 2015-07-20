package models

import play.api.libs.json._

/**
 * trydofor@moilioncircle.com 2015-07-20
 */

case class Alipay(id: String)


object Alipay {
  implicit val fm = Json.format[Alipay]
}
