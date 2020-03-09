package dev.shvimas.garcon.model

import dev.shvimas.garcon.database.Database
import dev.shvimas.garcon.database.model.UserData
import dev.shvimas.telegram.model.{Chat, Message}
import zio.ZIO

import scala.language.implicitConversions

object Text {
  final case class Checked private (value: String)   extends AnyVal
  final case class Unchecked private (value: String) extends AnyVal

  implicit def fromMessageText(mt: Message.Text): Unchecked = Unchecked(mt.value)
  implicit def fromString(s: String): Unchecked             = Unchecked(s)

  def prepareText(text: Unchecked, chatId: Chat.Id): ZIO[Database, Throwable, Checked] = {
    def decapitalize(maybeUserData: Option[UserData]): Checked = {
      val maybeTransformed: Option[String] =
        for {
          userData <- maybeUserData
          decap    <- userData.decapitalization
          if decap
        } yield text.value.toLowerCase() // TODO: do actual decapitalization instead of going all lower case?
      Checked(maybeTransformed.getOrElse(text.value))
    }

    for {
      maybeUserData <- ZIO.accessM[Database](_.getUserData(chatId))
      preparedText  <- ZIO.effect(decapitalize(maybeUserData))
    } yield preparedText
  }
}
