package dev.shvimas.telegram.model

import dev.shvimas.telegram.model.User._

case class User(id: Chat.Id,
                isBot: IsBot,
                firstName: FirstName,
                lastName: Option[LastName],
                username: Option[Username],
                languageCode: Option[LanguageCode])

object User {
  case class IsBot(value: Boolean)       extends AnyVal
  case class FirstName(value: String)    extends AnyVal
  case class LastName(value: String)     extends AnyVal
  case class Username(value: String)     extends AnyVal
  case class LanguageCode(value: String) extends AnyVal
}
