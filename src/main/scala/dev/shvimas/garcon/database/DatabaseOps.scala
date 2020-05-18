package dev.shvimas.garcon.database

import dev.shvimas.garcon.Database
import dev.shvimas.garcon.database.model.{CommonTranslation, UserData}
import dev.shvimas.garcon.model.Text
import dev.shvimas.telegram.Bot
import dev.shvimas.telegram.model.{Chat, Message}
import dev.shvimas.translate.LanguageDirection
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import zio.ZIO

object DatabaseOps {

  def updateOffset(offset: Bot.Offset): ZIO[Database, Throwable, UpdateResult] =
    ZIO.accessM[Database](_.get.updateOffset(offset))

  def getOffset: ZIO[Database, Throwable, Bot.Offset] =
    ZIO.accessM[Database](_.get.getOffset)

  def addCommonTranslation(translation: CommonTranslation,
                           chatId: Chat.Id,
                           languageDirection: LanguageDirection,
                           messageId: Message.Id,
  ): ZIO[Database, Throwable, UpdateResult] =
    ZIO.accessM[Database](_.get.addCommonTranslation(translation, chatId, languageDirection, messageId))

  def lookUpText(text: Text.Checked,
                 languageDirection: LanguageDirection,
                 chatId: Chat.Id,
  ): ZIO[Database, Throwable, Option[CommonTranslation]] =
    ZIO.accessM[Database](_.get.lookUpText(text, languageDirection, chatId))

  def getUserData(chatId: Chat.Id): ZIO[Database, Throwable, Option[UserData]] =
    ZIO.accessM[Database](_.get.getUserData(chatId))

  def setUserData(userData: UserData): ZIO[Database, Throwable, UpdateResult] =
    ZIO.accessM[Database](_.get.setUserData(userData))

  def setLanguageDirection(chatId: Chat.Id,
                           languageDirection: LanguageDirection,
  ): ZIO[Database, Throwable, UpdateResult] =
    ZIO.accessM[Database](_.get.setLanguageDirection(chatId, languageDirection))

  def findLanguageDirectionForMessage(chatId: Chat.Id,
                                      text: Text.Checked,
                                      messageId: Message.Id,
  ): ZIO[Database, Throwable, Option[LanguageDirection]] =
    ZIO.accessM[Database](_.get.findLanguageDirectionForMessage(chatId, text, messageId))

  def deleteText(text: Text.Checked,
                 languageDirection: LanguageDirection,
                 chatId: Chat.Id,
  ): ZIO[Database, Throwable, DeleteResult] =
    ZIO.accessM[Database](_.get.deleteText(text, languageDirection, chatId))

  def editTranslation(text: Text.Checked,
                      edit: String,
                      languageDirection: LanguageDirection,
                      chatId: Chat.Id,
  ): ZIO[Database, Throwable, Option[UpdateResult]] =
    ZIO.accessM[Database](_.get.editTranslation(text, edit, languageDirection, chatId))

  def getRandomWord(chatId: Chat.Id,
                    languageDirection: LanguageDirection,
  ): ZIO[Database, Throwable, Option[CommonTranslation]] =
    ZIO.accessM[Database](_.get.getRandomWord(chatId, languageDirection))
}
