package dev.shvimas.telegram.model

import dev.shvimas.telegram.model.Chat._

case class Chat(id: Id,
                `type`: Type,
                title: Option[Title],
                username: Option[Username],
                firstName: Option[FirstName],
                lastName: Option[LastName])

object Chat {
  case class Id(value: Long)          extends AnyVal
  case class Type(value: String)      extends AnyVal
  case class Title(value: String)     extends AnyVal
  case class Username(value: String)  extends AnyVal
  case class FirstName(value: String) extends AnyVal
  case class LastName(value: String)  extends AnyVal
}
