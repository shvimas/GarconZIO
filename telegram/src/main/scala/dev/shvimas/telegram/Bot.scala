package dev.shvimas.telegram

import dev.shvimas.telegram.model.InlineKeyboardMarkup
import dev.shvimas.telegram.model.Result.{GetUpdatesResult, SendMessageResult}

import scala.util.Try

trait Bot {
  def getUpdates(offset: Long): Try[GetUpdatesResult]

  def sendMessage(chatId: Int,
                  text: Option[String],
                  disableNotification: Boolean = true,
                  replyMarkup: Option[InlineKeyboardMarkup] = None,
                 ): Try[SendMessageResult]
}
