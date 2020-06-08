package dev.shvimas.garcon.database

import dev.shvimas.garcon.HasDatabase
import dev.shvimas.garcon.database.model.{CommonTranslation, UserData}
import dev.shvimas.garcon.model.Text
import dev.shvimas.telegram.Bot
import dev.shvimas.telegram.model.{Chat, Message}
import dev.shvimas.translate.LanguageDirection
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import zio.ZIO

object DatabaseOps {

  def updateOffset(offset: Bot.Offset): ZIO[HasDatabase, Throwable, UpdateResult] =
    ZIO.accessM[HasDatabase](_.get.updateOffset(offset))

  def getOffset: ZIO[HasDatabase, Throwable, Bot.Offset] =
    ZIO.accessM[HasDatabase](_.get.getOffset)

  def addCommonTranslation(translation: CommonTranslation,
                           chatId: Chat.Id,
                           languageDirection: LanguageDirection,
                           messageId: Message.Id,
  ): ZIO[HasDatabase, Throwable, UpdateResult] =
    ZIO.accessM[HasDatabase](_.get.addCommonTranslation(translation, chatId, languageDirection, messageId))

  def lookUpText(text: Text.Checked,
                 languageDirection: LanguageDirection,
                 chatId: Chat.Id,
  ): ZIO[HasDatabase, Throwable, Option[CommonTranslation]] =
    ZIO.accessM[HasDatabase](_.get.lookUpText(text, languageDirection, chatId))

  def getUserData(chatId: Chat.Id): ZIO[HasDatabase, Throwable, Option[UserData]] =
    ZIO.accessM[HasDatabase](_.get.getUserData(chatId))

  def setUserData(userData: UserData): ZIO[HasDatabase, Throwable, UpdateResult] =
    ZIO.accessM[HasDatabase](_.get.setUserData(userData))

  def setLanguageDirection(chatId: Chat.Id,
                           languageDirection: LanguageDirection,
  ): ZIO[HasDatabase, Throwable, UpdateResult] =
    ZIO.accessM[HasDatabase](_.get.setLanguageDirection(chatId, languageDirection))

  def findLanguageDirectionForMessage(chatId: Chat.Id,
                                      text: Text.Checked,
                                      messageId: Message.Id,
  ): ZIO[HasDatabase, Throwable, Option[LanguageDirection]] =
    ZIO.accessM[HasDatabase](_.get.findLanguageDirectionForMessage(chatId, text, messageId))

  def deleteText(text: Text.Checked,
                 languageDirection: LanguageDirection,
                 chatId: Chat.Id,
  ): ZIO[HasDatabase, Throwable, DeleteResult] =
    ZIO.accessM[HasDatabase](_.get.deleteText(text, languageDirection, chatId))

  def editTranslation(text: Text.Checked,
                      edit: String,
                      languageDirection: LanguageDirection,
                      chatId: Chat.Id,
  ): ZIO[HasDatabase, Throwable, Option[UpdateResult]] =
    ZIO.accessM[HasDatabase](_.get.editTranslation(text, edit, languageDirection, chatId))

  def getRandomWord(chatId: Chat.Id,
                    languageDirection: LanguageDirection,
  ): ZIO[HasDatabase, Throwable, Option[CommonTranslation]] =
    ZIO.accessM[HasDatabase](_.get.getRandomWord(chatId, languageDirection))
}
