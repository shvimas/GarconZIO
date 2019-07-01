package dev.shvimas.garcon.database

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.StrictLogging
import dev.shvimas.garcon.database.model.{CommonTranslation, UserData}
import dev.shvimas.garcon.TestHelpers._
import dev.shvimas.translate.LanguageDirection
import org.mongodb.scala.result.UpdateResult
import scalaz.zio.{Task, ZIO}

import scala.collection.concurrent
import scala.collection.JavaConverters._

trait TestDatabase extends Database with StrictLogging {
  val offset: AtomicReference[Option[Long]] =
    new AtomicReference(None)

  val userData: concurrent.Map[Int, UserData] =
    new ConcurrentHashMap[Int, UserData]().asScala

  val savedTranslations: concurrent.Map[(Int, LanguageDirection), concurrent.Map[String, CommonTranslation]] =
    new ConcurrentHashMap[(Int, LanguageDirection), concurrent.Map[String, CommonTranslation]]().asScala

  def initTestDatabase(offset: Option[Long],
                       userData: Map[Int, UserData],
                       translations: Map[(Int, LanguageDirection), concurrent.Map[String, CommonTranslation]],
                      ): Unit = {
    this.offset.set(offset)
    this.userData.clear()
    this.userData ++= userData
    this.savedTranslations.clear()
    this.savedTranslations ++= translations
  }

  override def updateOffset(offset: Long): Task[UpdateResult] =
    ZIO.effect {
      val matchedCount: Long = if (this.offset.get().nonEmpty) 1 else 0
      val modifiedCount: Long =
        if (this.offset.get().getOrElse(-1L) < offset) {
          this.offset.set(Some(offset))
          logger.info(s"Updated offset to $offset")
          1
        } else 0

      makeUpdateResult(
        matchedCount = matchedCount,
        modifiedCount = modifiedCount,
      )
    }

  override def getOffset: Task[Long] =
    ZIO.effect(offset.get().getOrElse(0))

  override def getUserData(chatId: Int): Task[Option[UserData]] =
    ZIO.effect(userData.get(chatId))

  override def setUserData(userData: UserData): Task[UpdateResult] =
    ZIO.effect({
      val (matched, modified) = this.userData.get(userData.chatId) match {
        case None => 0 -> 0
        case Some(oldUserData) =>
          if (oldUserData == userData)
            1 -> 0
          else 1 -> 1
      }
      this.userData(userData.chatId) = userData
      logger.info(s"Set user data for chat id: ${userData.chatId}")
      makeUpdateResult(
        matchedCount = matched,
        modifiedCount = modified,
      )
    })

  override def setLanguageDirection(chatId: Int, languageDirection: LanguageDirection): Task[UpdateResult] =
    ZIO.effect {
      val ((matched, modified), replacement) = this.userData.get(chatId) match {
        case None =>
          0 -> 0 -> UserData(chatId, Some(languageDirection))
        case Some(UserData(_, None)) =>
          1 -> 0 -> UserData(chatId, Some(languageDirection))
        case Some(UserData(_, Some(oldLanguageDirection))) =>
          if (oldLanguageDirection == languageDirection)
            1 -> 0 -> UserData(chatId, Some(languageDirection))
          else
            1 -> 1 -> UserData(chatId, Some(languageDirection))

      }
      this.userData(chatId) = replacement
      logger.info(s"Set $languageDirection as translator to $chatId")
      makeUpdateResult(
        matchedCount = matched,
        modifiedCount = modified,
      )
    }

  override def addCommonTranslation(translation: CommonTranslation,
                                    key: (Int, LanguageDirection),
                                   ): Task[UpdateResult] =
    ZIO.effect {
        val text = translation.originalText
        val (matchedCount, modifiedCount) =
          savedTranslations.get(key) match {
            case None =>
              val newColl = new ConcurrentHashMap[String, CommonTranslation].asScala += (text -> translation)
              savedTranslations += key -> newColl
              0 -> 0
            case Some(existingTranslations) =>
              existingTranslations.get(text) match {
                case None =>
                  existingTranslations += text -> translation
                  0 -> 0
                case Some(oldCommonTranslation) =>
                  if (oldCommonTranslation.translations != translation.translations) {
                    existingTranslations += text -> translation
                    1 -> 1
                  } else {
                    1 -> 0
                  }
              }
          }
        logger.info(s"Added $translation to $key")
        makeUpdateResult(
          matchedCount = matchedCount,
          modifiedCount = modifiedCount,
        )
      }
}
