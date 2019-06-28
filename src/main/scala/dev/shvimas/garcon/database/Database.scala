package dev.shvimas.garcon.database

import dev.shvimas.garcon.database.model._
import dev.shvimas.garcon.database.response._
import dev.shvimas.translate.LanguageDirection
import scalaz.zio.Task

trait Database {
  def updateOffset(offset: Long): Task[UpdateResult]

  def getOffset: Task[Long]

  def addCommonTranslation(translation: CommonTranslation, key: (Int, LanguageDirection)): Task[UpdateResult]

  def getUserData(chatId: Int): Task[Option[UserData]]

  def setUserData(userData: UserData): Task[UpdateResult]

  def setLanguageDirection(chatId: Int, languageDirection: LanguageDirection): Task[UpdateResult]
}
