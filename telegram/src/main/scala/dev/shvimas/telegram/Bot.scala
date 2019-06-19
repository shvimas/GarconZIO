package dev.shvimas.telegram

import dev.shvimas.telegram.model.Result.{GetUpdatesResult, SendMessageResult}

import scala.util.Try

trait Bot {
  def getUpdates(offset: Long): Try[GetUpdatesResult]

  def sendMessage(chatId: Int,
                  text: Option[String],
                  disableNotification: Boolean = true,
  ): Try[SendMessageResult]
}
