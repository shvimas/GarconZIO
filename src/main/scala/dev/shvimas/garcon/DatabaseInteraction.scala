package dev.shvimas.garcon

import cats.syntax.show._
import com.typesafe.scalalogging.StrictLogging
import dev.shvimas.garcon.database.Database
import dev.shvimas.garcon.utils.ExceptionUtils.showThrowable
import dev.shvimas.garcon.Main._
import dev.shvimas.garcon.database.model.UserData
import dev.shvimas.garcon.model.{DecapitalizeCommand, _}
import dev.shvimas.telegram.model.Result.GetUpdatesResult
import dev.shvimas.translate.LanguageDirection
import org.mongodb.scala.result.UpdateResult
import scalaz.zio.ZIO

object DatabaseInteraction extends StrictLogging {

  import CommonUtils._

  def updateOffset(results: GetUpdatesResult): ZIO[Database, Nothing, Unit] =
    ZIO.succeed(maxUpdateId(results) + 1)
      .flatMap(doOffsetUpdate)
      .mapError {
        case (throwable: Throwable, offset: Long) =>
          logger.error(
            s"""While updating offset ($offset):
               |${throwable.show}""".stripMargin)
      }.option.unit

  private def doOffsetUpdate(maxId: Long): ZIO[Database, (Throwable, Long), Unit] =
    if (maxId >= 0) {
      ZIO.accessM[Database](_.updateOffset(maxId))
        .map(updateResult => require(updateResult.getModifiedCount == 1))
        .mapError(err => err -> maxId)
    } else {
      ZIO.unit
    }

  private def maxUpdateId(result: GetUpdatesResult): Long =
    result.result
      .map(_.updateId)
      .fold(-1)(math.max)

  def resolveLangDirection(chatId: Int): ZIO[Database, Throwable, LanguageDirection] =
    ZIO.accessM[Database](_.getUserData(chatId))
      .flatMap {
        case Some(UserData(_, Some(languageDirection), _)) =>
          ZIO.succeed(languageDirection)
        case Some(UserData(_, None, _)) =>
          ZIO.accessM[Database](_.setLanguageDirection(chatId, Defaults.languageDirection)) *> ZIO.succeed(Defaults.languageDirection)
        case None =>
          ZIO.accessM[Database](_.setUserData(Defaults.userData(chatId))) *> ZIO.succeed(Defaults.languageDirection)
      }

  def saveResults(allResults: AllResults): ZIO[Database, Nothing, Unit] =
    ZIO.collectAllPar(
      allResults.flatMap { case (chatId, perUserResults: List[Either[ErrorWithInfo, Response]]) =>
        perUserResults.map {
          case Left(_) => ZIO.unit
          case Right(response) =>
            response match {
              case TranslationResponse(translationWithInfo) =>
                saveTranslationResult(chatId, translationWithInfo)
              case DecapitalizeResponse(state) =>
                saveDecapitalizationState(chatId, state)
              case _: DeletionResponse |
                   _: TestResponse |
                   _: ChooseResponse |
                   HelpResponse |
                   _: ErrorResponse => ZIO.unit
            }
        }
      }
    ).map(unify)

  private def saveDecapitalizationState(chatId: Int,
                                        state: DecapitalizeCommand.State.Value,
                                       ): ZIO[Database, Nothing, Unit] = {
    val decapValue: Boolean =
      state match {
        case DecapitalizeCommand.State.ON => true
        case DecapitalizeCommand.State.OFF => false
      }
    ZIO
      .accessM[Database](_.getUserData(chatId))
      .mapError("While getting user data" -> _)
      .flatMap { maybeUserData: Option[UserData] =>
        val userData: UserData =
          maybeUserData match {
            case Some(oldUserData) => oldUserData.copy(decapitalization = Some(decapValue))
            case None => Defaults.userData(chatId, decapitalization = decapValue)
          }
        ZIO
          .accessM[Database](_.setUserData(userData))
          .mapError("While updating decap state:" -> _)
      }
      .fold(logErrorWithContext, _ => ())
  }

  private def saveTranslationResult(chatId: Int,
                                    translationWithInfo: TranslationWithInfo,
                                   ): ZIO[Database, Nothing, Unit] = {
    val TranslationWithInfo(commonTranslation, languageDirection, messageId) = translationWithInfo
    if (commonTranslation.isEmpty) {
      ZIO.unit
    } else {
      ZIO
        .accessM[Database](_.addCommonTranslation(commonTranslation, chatId, languageDirection, messageId))
        .map { updateResult: UpdateResult =>
          if (!updateResult.wasAcknowledged) {
            logger.error(s"Tried to save $commonTranslation for $chatId but it was not acknowledged")
          }
        }
        .mapError(s"While saving $commonTranslation for $chatId:" -> _)
        .fold(logErrorWithContext, _ => ())
    }
  }
}
