package dev.shvimas.garcon.database

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.StrictLogging
import dev.shvimas.garcon.database.model.{CommonTranslation, CommonTranslationFields, UserData}
import dev.shvimas.garcon.database.response.{Completed, UpdateResult}
import dev.shvimas.translate.{LanguageDirection, Translation}
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

      UpdateResult(
        wasAcknowledged = true,
        matchedCount = matchedCount,
        modifiedCount = modifiedCount,
      )
    }

  override def getOffset: Task[Long] =
    ZIO.effect(offset.get().getOrElse(0))

  override def getUserData(chatId: Int): Task[Option[UserData]] =
    ZIO.effect(userData.get(chatId))

  override def setUserData(userData: UserData): Task[Completed] =
    ZIO.effect({
      this.userData(userData.chatId) = userData
      logger.info(s"Set default user data for chat id: ${userData.chatId}")
      Completed()
    })

  override def addText(translation: Translation,
                       translatorName: String,
                       key: (Int, LanguageDirection),
                      ): Task[UpdateResult] =
    ZIO.succeed(CommonTranslation.from(translation, translatorName))
      .map {
        case Some(commonTranslation) =>
          val text = translation.originalText
          val (matchedCount, modifiedCount) =
            savedTranslations.get(key) match {
              case None =>
                val newColl = new ConcurrentHashMap[String, CommonTranslation].asScala += (text -> commonTranslation)
                savedTranslations += key -> newColl
                0 -> 0
              case Some(existingTranslations) =>
                existingTranslations.get(text) match {
                  case None =>
                    existingTranslations += text -> commonTranslation
                    0 -> 0
                  case Some(oldCommonTranslation) =>
                    val maybeOldTranslatedText: Option[String] =
                      translatorName match {
                        case CommonTranslationFields.abbyy => oldCommonTranslation.abbyy
                        case CommonTranslationFields.yandex => oldCommonTranslation.yandex
                        case other => throw new RuntimeException(s"Unknown translator: $other")
                      }
                    maybeOldTranslatedText match {
                      case None =>
                        existingTranslations += text -> commonTranslation
                        1 -> 1
                      case Some(oldTranslatedText) =>
                        if (oldTranslatedText != translation.translatedText) {
                          existingTranslations += text -> commonTranslation
                          1 -> 1
                        } else 1 -> 0
                    }
                }
            }
          logger.info(s"Added $commonTranslation to $key")
          UpdateResult(
            wasAcknowledged = true,
            matchedCount = matchedCount,
            modifiedCount = modifiedCount,
          )
        case None =>
          UpdateResult(
            wasAcknowledged = false,
            matchedCount = 0,
            modifiedCount = 0,
          )
      }
}
