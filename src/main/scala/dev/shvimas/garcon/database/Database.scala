package dev.shvimas.garcon.database

import dev.shvimas.garcon.database.model._
import dev.shvimas.garcon.model.Text
import dev.shvimas.telegram.model.{Chat, Message}
import dev.shvimas.telegram.Bot
import dev.shvimas.translate.LanguageDirection
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import zio.Task

object Database {

  trait Service {
    def updateOffset(offset: Bot.Offset): Task[UpdateResult]

    def getOffset: Task[Bot.Offset]

    def addCommonTranslation(translation: CommonTranslation,
                             chatId: Chat.Id,
                             languageDirection: LanguageDirection,
                             messageId: Message.Id,
    ): Task[UpdateResult]

  def lookUpText(text: Text.Checked,
                 languageDirection: LanguageDirection,
                 chatId: Chat.Id,
    ): Task[Option[CommonTranslation]]

    def getUserData(chatId: Chat.Id): Task[Option[UserData]]

    def setUserData(userData: UserData): Task[UpdateResult]

    def setLanguageDirection(chatId: Chat.Id, languageDirection: LanguageDirection): Task[UpdateResult]

  def findLanguageDirectionForMessage(chatId: Chat.Id,
                                      text: Text.Checked,
                                      messageId: Message.Id,
    ): Task[Option[LanguageDirection]]

    def deleteText(text: Text.Checked, langDirection: LanguageDirection, chatId: Chat.Id): Task[DeleteResult]

    def editTranslation(text: Text.Checked,
                        edit: String,
                        languageDirection: LanguageDirection,
                        chatId: Chat.Id,
    ): Task[Option[UpdateResult]]

    def getRandomWords(chatId: Chat.Id,
                     languageDirection: LanguageDirection,
                     numWords: Int): Task[Seq[CommonTranslation]]
  }
}
