package dev.shvimas.garcon

import dev.shvimas.telegram.Bot.Offset
import dev.shvimas.telegram.model._
import dev.shvimas.telegram.Bot
import zio.{Has, ZIO}

object BotOps {

  def getUpdates(offset: Offset): ZIO[Has[Bot], Throwable, GetUpdatesResult] =
    ZIO.accessM[Has[Bot]](bot => ZIO.fromTry(bot.get.getUpdates(offset)))

  def sendMessage(chatId: Chat.Id,
                  text: Option[String],
                  replyMarkup: Option[InlineKeyboardMarkup],
                  disableNotification: Boolean,
  ): ZIO[Has[Bot], Throwable, SendMessageResult] =
    ZIO.accessM[Has[Bot]](
        bot =>
          ZIO.fromTry(
              bot.get.sendMessage(
                  chatId = chatId,
                  text = text,
                  replyMarkup = replyMarkup,
                  disableNotification = disableNotification
              )
        )
    )
}
