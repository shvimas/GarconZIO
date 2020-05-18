package dev.shvimas.garcon

import com.typesafe.scalalogging.StrictLogging
import dev.shvimas.garcon.database.DatabaseOps
import dev.shvimas.garcon.Main._
import dev.shvimas.garcon.database.model.UserData
import dev.shvimas.garcon.model.{DecapitalizeCommand, _}
import dev.shvimas.garcon.CommonUtils.logErrorWithContext
import dev.shvimas.telegram.model.{Chat, GetUpdatesResult, Update}
import dev.shvimas.telegram.Bot
import dev.shvimas.translate.LanguageDirection
import org.mongodb.scala.result.UpdateResult
import zio.{Task, ZIO}

object DatabaseInteraction extends StrictLogging {

  def updateOffset(updatesResult: GetUpdatesResult): ZIO[Database, Nothing, Unit] =
    (for {
      offset <- newOffset(updatesResult).mapError(logErrorWithContext(s"While calculating new offset", _))
      _      <- doOffsetUpdate(offset).mapError(logErrorWithContext(s"While updating offset $offset", _))
    } yield ()).fold(_ => (), _ => ())

  private def doOffsetUpdate(offset: Bot.Offset): ZIO[Database, Throwable, Unit] =
    ZIO.when(offset > 0) {
      for {
        result <- DatabaseOps.updateOffset(offset)
        _      <- ZIO.effect(require(result.getModifiedCount == 1))
      } yield ()
    }

  private def newOffset(result: GetUpdatesResult): Task[Bot.Offset] =
    ZIO.effect {
      val maybeMaxUpdateId: Option[Update.Id] =
        result.result
          .reduceOption[Update] { case (u1, u2) => if (u1.updateId.value >= u2.updateId.value) u1 else u2 }
          .map(_.updateId)
      Bot.Offset(maybeMaxUpdateId.map(id => id.value + 1).getOrElse(0))
    }

  def resolveLangDirection(chatId: Chat.Id): ZIO[Database, Throwable, LanguageDirection] =
    DatabaseOps.getUserData(chatId).flatMap {
      case Some(UserData(_, Some(languageDirection), _)) =>
        ZIO.succeed(languageDirection)
      case Some(UserData(_, None, _)) =>
        DatabaseOps.setLanguageDirection(chatId, Defaults.languageDirection).as(Defaults.languageDirection)
      case None =>
        DatabaseOps.setUserData(Defaults.userData(chatId)).as(Defaults.languageDirection)
    }

  def saveResults(allResults: AllResults): ZIO[Database, Nothing, Unit] = {
    val saveEffects: List[ZIO[Database, Nothing, Unit]] = for {
      (chatId, perUserResults) <- allResults
      result                   <- perUserResults
    } yield
      result match {
        case Left(_) => ZIO.unit
        case Right(response) =>
          response match {
            case TranslationResponse(translationWithInfo) =>
              saveTranslationResult(chatId, translationWithInfo)
            case DecapitalizeResponse(state) =>
              saveDecapitalizationState(chatId, state)
            case _: DeletionResponse | _: TestResponse | _: ChooseResponse | _: EditResponse | HelpResponse |
                _: ErrorResponse =>
              ZIO.unit
          }
      }

    ZIO.collectAllSuccessesPar(saveEffects).unit
  }

  private def saveDecapitalizationState(chatId: Chat.Id,
                                        state: DecapitalizeCommand.State.Value,
  ): ZIO[Database, Nothing, Unit] = {
    val decapValue: Boolean =
      state match {
        case DecapitalizeCommand.State.ON  => true
        case DecapitalizeCommand.State.OFF => false
      }
    DatabaseOps
      .getUserData(chatId)
      .mapError("While getting user data" -> _)
      .flatMap { maybeUserData: Option[UserData] =>
        val userData: UserData =
          maybeUserData match {
            case Some(oldUserData) => oldUserData.copy(decapitalization = Some(decapValue))
            case None              => Defaults.userData(chatId, decapitalization = decapValue)
          }
        DatabaseOps
          .setUserData(userData)
          .mapError("While updating decap state:" -> _)
      }
      .fold(logErrorWithContext, _ => ())
  }

  private def saveTranslationResult(chatId: Chat.Id,
                                    translationWithInfo: TranslationWithInfo,
  ): ZIO[Database, Nothing, Unit] = {
    val TranslationWithInfo(commonTranslation, languageDirection, messageId) = translationWithInfo
    ZIO
      .when(commonTranslation.nonEmpty) {
        DatabaseOps
          .addCommonTranslation(commonTranslation, chatId, languageDirection, messageId)
          .flatMap { updateResult: UpdateResult =>
            ZIO.when(!updateResult.wasAcknowledged) {
              ZIO.effect(logger.error(s"Tried to save $commonTranslation for $chatId but it was not acknowledged"))
            }
          }
      }
      .mapError(s"While saving $commonTranslation for $chatId:" -> _)
      .fold(logErrorWithContext, _ => ())
  }
}
