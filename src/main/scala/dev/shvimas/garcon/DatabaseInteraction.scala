package dev.shvimas.garcon

import dev.shvimas.garcon.database.DatabaseOps
import dev.shvimas.garcon.Main._
import dev.shvimas.garcon.database.model.UserData
import dev.shvimas.garcon.model.{DecapitalizeCommand, _}
import dev.shvimas.telegram.model.{Chat, GetUpdatesResult, Update}
import dev.shvimas.telegram.Bot
import dev.shvimas.translate.LanguageDirection
import dev.shvimas.ZIOLogging
import org.mongodb.scala.result.UpdateResult
import zio.{Task, ZIO}

object DatabaseInteraction extends ZIOLogging {

  def updateOffset(updatesResult: GetUpdatesResult): ZIO[HasDatabase, Nothing, Unit] =
    (for {
      offset <- newOffset(updatesResult).logOnError("While calculating new offset")
      _      <- doOffsetUpdate(offset).logOnError(s"While updating offset $offset")
    } yield ()).orElseSucceed(())

  private def doOffsetUpdate(offset: Bot.Offset): ZIO[HasDatabase, Throwable, Unit] =
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

  def resolveLangDirection(chatId: Chat.Id): ZIO[HasDatabase, Throwable, LanguageDirection] =
    DatabaseOps.getUserData(chatId).flatMap {
      case Some(UserData(_, Some(languageDirection), _)) =>
        ZIO.succeed(languageDirection)
      case Some(UserData(_, None, _)) =>
        DatabaseOps.setLanguageDirection(chatId, Defaults.languageDirection).as(Defaults.languageDirection)
      case None =>
        DatabaseOps.setUserData(Defaults.userData(chatId)).as(Defaults.languageDirection)
    }

  def saveResults(allResults: AllResults): ZIO[HasDatabase, Nothing, Unit] = {
    val saveEffects: List[ZIO[HasDatabase, Nothing, Unit]] = for {
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
  ): ZIO[HasDatabase, Nothing, Unit] = {
    val decapValue: Boolean =
      state match {
        case DecapitalizeCommand.State.ON  => true
        case DecapitalizeCommand.State.OFF => false
      }
    DatabaseOps
      .getUserData(chatId)
      .logOnError("While getting user data")
      .flatMap { maybeUserData: Option[UserData] =>
        val userData: UserData =
          maybeUserData match {
            case Some(oldUserData) => oldUserData.copy(decapitalization = Some(decapValue))
            case None              => Defaults.userData(chatId, decapitalization = decapValue)
          }
        DatabaseOps
          .setUserData(userData)
          .logOnError("While updating decap state")
          .unit
      }
      .orElseSucceed(())
  }

  private def saveTranslationResult(chatId: Chat.Id,
                                    translationWithInfo: TranslationWithInfo,
  ): ZIO[HasDatabase, Nothing, Unit] = {
    val TranslationWithInfo(commonTranslation, languageDirection, messageId) = translationWithInfo
    ZIO
      .when(commonTranslation.nonEmpty) {
        DatabaseOps
          .addCommonTranslation(commonTranslation, chatId, languageDirection, messageId)
          .flatMap { updateResult: UpdateResult =>
            ZIO.when(!updateResult.wasAcknowledged) {
              zioLogger.error(s"Tried to save $commonTranslation for $chatId but it was not acknowledged")
            }
          }
      }
      .logOnError(s"While saving $commonTranslation for $chatId")
      .orElseSucceed(())
  }
}
