package dev.shvimas.garcon.database

import dev.shvimas.garcon.database.model._
import dev.shvimas.translate.LanguageDirection
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import scalaz.zio.Task

trait Database {
  def updateOffset(offset: Long): Task[UpdateResult]

  def getOffset: Task[Long]

  def addCommonTranslation(translation: CommonTranslation,
                           chatId: Int,
                           languageDirection: LanguageDirection,
                           messageId: Int,
                          ): Task[UpdateResult]

  def getUserData(chatId: Int): Task[Option[UserData]]

  def setUserData(userData: UserData): Task[UpdateResult]

  def setLanguageDirection(chatId: Int, languageDirection: LanguageDirection): Task[UpdateResult]

  def findLanguageDirectionForMessage(chatId: Int, text: String, messageId: Int): Task[Option[LanguageDirection]]

  def deleteText(text: String, langDirection: LanguageDirection, chatId: Int): Task[DeleteResult]
}
