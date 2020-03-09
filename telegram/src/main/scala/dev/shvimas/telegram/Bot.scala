package dev.shvimas.telegram

import dev.shvimas.telegram.model._
import dev.shvimas.telegram.Bot._

import scala.util.Try

trait Bot {
  def getUpdates(offset: Offset): Try[GetUpdatesResult]

  def sendMessage(chatId: Chat.Id,
                  text: Option[String],
                  disableNotification: Boolean = true,
                  replyMarkup: Option[InlineKeyboardMarkup] = None,
  ): Try[SendMessageResult]
}

object Bot {
  case class Offset(value: Long) extends AnyVal {
    def +(l: Long): Offset = new Offset(value + l)
    def >(l: Long): Boolean = value > l
  }
}
