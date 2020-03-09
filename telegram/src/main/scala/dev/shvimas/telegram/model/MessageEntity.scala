package dev.shvimas.telegram.model

import dev.shvimas.telegram.model.MessageEntity._

case class MessageEntity(`type`: Type, offset: Offset, length: Length, url: Option[URL], user: Option[User])

object MessageEntity {
  case class Type(value: String) extends AnyVal
  case class Offset(value: Int)  extends AnyVal
  case class Length(value: Int)  extends AnyVal
  case class URL(value: String)  extends AnyVal
}
