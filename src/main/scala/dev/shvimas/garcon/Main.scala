package dev.shvimas.garcon

import cats.syntax.show._
import com.typesafe.scalalogging.LazyLogging
import dev.shvimas.garcon.database.Database
import dev.shvimas.garcon.database.model.UserData
import dev.shvimas.garcon.model._
import dev.shvimas.garcon.utils.ExceptionUtils.showThrowable
import dev.shvimas.telegram._
import dev.shvimas.telegram.model.Result.{GetUpdatesResult, SendMessageResult}
import dev.shvimas.telegram.model.Update
import dev.shvimas.translate.LanguageDirection
import scalaz.zio.{ZIO, _}
import scalaz.zio.clock.Clock
import scalaz.zio.duration._

object Main extends App with LazyLogging {

  import dev.shvimas.garcon.BotInteraction._
  import dev.shvimas.garcon.CommonUtils._
  import dev.shvimas.garcon.DatabaseInteraction._
  import dev.shvimas.garcon.MainConfig._
  import dev.shvimas.garcon.TranslatorsInteraction._

  type GarconEnvironment = Bot with Database with Translators

  override def run(args: List[String]): ZIO[Any, Nothing, Int] = {
    main
      .provide(environment)
      .repeat(ZSchedule.fixed(200.milliseconds))
      .provide(Clock.Live)
      .fold((_: Nothing) => 1, (_: Int) => 0)
  }

  val main: ZIO[GarconEnvironment, Nothing, Unit] =
    getUpdates.flatMap(processUpdates)

  def getUpdates: ZIO[Bot with Database, Nothing, Option[GetUpdatesResult]] =
    ZIO.accessM[Database](_.getOffset)
      .flatMap(offset =>
        ZIO.accessM[Bot](bot => ZIO.fromTry(bot.getUpdates(offset)))
      ).mapError(throwable =>
      logger.error(
        s"""While getting updates:
           |${throwable.show}""".stripMargin)
    ).option

  def processUpdates(maybeResults: Option[GetUpdatesResult]): ZIO[GarconEnvironment, Nothing, Unit] =
    maybeResults match {
      case Some(results) => (updateOffset(results) <&> respondToUpdates(results)).map(unify)
      case None => ZIO.unit
    }

  def respondToUpdates(getUpdatesResult: GetUpdatesResult): ZIO[GarconEnvironment, Nothing, Unit] =
    groupUpdates(getUpdatesResult)
      .flatMap(processGroupedUpdates)
      .flatMap(results =>
        ZIO.collectAllPar(List(
          saveResults(results),
          processResults(results),
          processErrors(results))))
      .map(unify)

  def groupUpdates(getUpdatesResult: GetUpdatesResult): UIO[Map[Int, List[Update]]] =
    ZIO.effectTotal {
      getUpdatesResult.result
        .groupBy(_.chatId)
        .map {
          case (Some(chatId), updates) => Some(chatId -> updates)
          case (None, updates) => unify(processOrphanUpdates(updates), Option.empty[(Int, List[Update])])
        }
        .flatten
        .toMap
    }

  def processResults(results: AllResults): ZIO[Bot with Database, Nothing, Unit] =
    sendResponses(results)
      .flatMap { sendResponsesResult: Seq[(Int, Either[Throwable, List[SendMessageResult]])] =>
        ZIO.collectAll(sendResponsesResult.map {
          case (chatId, Left(throwable: Throwable)) =>
            ZIO.effectTotal(
              logger.error(
                s"""Failed to send responses to $chatId:
                   |${throwable.show}""".stripMargin))
          case (chatId, Right(sendMessageResults: List[SendMessageResult])) =>
            // TODO: resend if not ok?
            ZIO.effectTotal(
              logger.info(
                s"""Send message results for $chatId:
                   |${sendMessageResults.mkString("\n")}
                   |""".stripMargin)
            )
        }
        ).map(unify)
      }

  def processTranslationRequest(request: TranslationRequest,
                               ): ZIO[Database with Translators, Throwable, TranslationResponse] = {
    for {
      text <- prepareText(request.text, request.chatId)
      languageDirection <- resolveLangDirection(request.chatId).map(_.maybeReverse(text))
      commonTranslation <- commonTranslation(text, languageDirection)
    } yield {
      TranslationResponse(
        TranslationWithInfo(
          translation = commonTranslation,
          languageDirection = languageDirection,
          messageId = request.messageId,
        )
      )
    }
  }

  def prepareText(text: String, chatId: Int): ZIO[Database, Throwable, String] = {
    def decapitalize(maybeUserData: Option[UserData]): String = {
      val maybeTransformed: Option[String] =
        for {
          userData <- maybeUserData
          decap <- userData.decapitalization
          if decap
        } yield text.toLowerCase()
      maybeTransformed.getOrElse(text)
    }

    for {
      maybeUserData <- ZIO.accessM[Database](_.getUserData(chatId))
      preparedText <- ZIO.effect(decapitalize(maybeUserData))
    } yield preparedText
  }

  def processDeleteCommand(command: DeleteCommand): ZIO[Database, Throwable, DeletionResponse] = {
    def deleteText(text: String,
                   languageDirection: LanguageDirection,
                   chatId: Int
                  ): ZIO[Database, Throwable, Either[String, String]] = {
      ZIO.accessM[Database](_.deleteText(text, languageDirection, chatId))
        .map { result =>
          if (result.wasAcknowledged()) {
            Right(s"Deleted $text ($languageDirection)")
          } else {
            Left(s"Failed to delete $text ($languageDirection)")
          }
        }
    }

    val result: ZIO[Database, Throwable, Either[String, String]] =
      command match {
        case DeleteByReply(reply, chatId) =>
          reply.text.map(prepareText(_, chatId)) match {
            case Some(zText) =>
              zText.flatMap { text: String =>
                ZIO.accessM[Database](_.findLanguageDirectionForMessage(chatId, text, reply.messageId))
                  .flatMap {
                    case Some(languageDirection) =>
                      deleteText(text, languageDirection, chatId)
                    case None =>
                      ZIO.succeed(Left(s"Couldn't delete $text (failed to find language direction)"))
                  }
              }
            case None =>
              ZIO.succeed(Left("Couldn't delete (text is empty)"))
          }
        case DeleteByText(text, languageDirection, chatId) =>
          for {
            preparedText <- prepareText(text, chatId)
            response <- deleteText(preparedText, languageDirection, chatId)
          } yield response
      }
    result.map(DeletionResponse)
  }

  def processTestCommand(command: TestCommand): ZIO[Database, Throwable, TestResponse] = {
    command match {
      case TestStartCommand(languageDirection, chatId) =>
        ZIO.accessM[Database](_.getRandomWord(chatId, languageDirection))
          .map(TestStartResponse(_, languageDirection))
      case TestNextCommand(languageDirection, chatId) =>
        ZIO.accessM[Database](_.getRandomWord(chatId, languageDirection))
          .map(TestNextResponse(_, languageDirection))
      case TestShowCommand(text, languageDirection, chatId) =>
        ZIO.accessM[Database](_.lookUpText(text, languageDirection, chatId))
          .map(TestShowResponse(_, languageDirection))
    }
  }

  def processChooseRequest(command: ChooseCommand): ZIO[Database, Throwable, ChooseResponse] = {
    command match {
      case ChooseCommand(languageDirection, chatId) =>
        ZIO.accessM[Database](_.setLanguageDirection(chatId, languageDirection))
          .map { updateResult =>
            if (updateResult.wasAcknowledged()) {
              SuccessfulChooseResponse(languageDirection)
            } else {
              FailedChooseResponse("saving language direction was not acknowledged", languageDirection)
            }
          }
    }
  }

  def processUpdate(update: Update): ZIO[Database with Translators, Throwable, Response] =
    RequestParser.parseUpdate(update) match {
      case request: TranslationRequest =>
        processTranslationRequest(request)
      case command: DeleteCommand =>
        processDeleteCommand(command)
      case command: TestCommand =>
        processTestCommand(command)
      case command: ChooseCommand =>
        processChooseRequest(command)
      case DecapitalizeCommand(state) =>
        ZIO.succeed(DecapitalizeResponse(state))
      case HelpCommand =>
        ZIO.succeed(HelpResponse)
      case MalformedCommand(desc) =>
        ZIO.succeed(MalformedCommandResponse(desc))
      case UnrecognisedCommand(command) =>
        ZIO.succeed(UnrecognisedCommandResponse(command))
      case EmptyUpdate =>
        ZIO.succeed(EmptyUpdateResponse)
      case EmptyMessage =>
        ZIO.succeed(EmptyMessageResponse)
      case EmptyCallbackData =>
        ZIO.succeed(EmptyCallbackDataResponse)
    }

  case class ErrorWithInfo(error: Throwable, update: Update)

  type AllResults = List[(Int, List[Either[ErrorWithInfo, Response]])]

  def processGroupedUpdates(updateGroups: Map[Int, Seq[Update]],
                           ): ZIO[Database with Translators, Nothing, AllResults] =
  // important that error type is Nothing in inner collect
  // otherwise collectAllPar could interrupt other users' processing
    ZIO.collectAllPar(
      updateGroups.map { case (chatId, updates) =>
        ZIO.collectAll(
          updates.map(update =>
            processUpdate(update)
              .mapError(ErrorWithInfo(_, update))
              .either))
          .map(chatId -> _)
      }
    )

  private def processOrphanUpdates(orphanUpdates: Seq[Update]): Unit = {
    logger.warn(
      s"""Got updates from unknown chat:
         |${orphanUpdates.mkString("\n")}
         |
         |
         |These updates were left unattended""".stripMargin)
  }

  def processErrors(allResults: AllResults): ZIO[Any, Nothing, Unit] =
    ZIO.foreachPar(allResults) {
      case (_, resultsPerUser: List[Either[ErrorWithInfo, Response]]) =>
        resultsPerUser.foreach {
          case Left(ErrorWithInfo(throwable, update)) =>
            logger.error(
              s"""While processing $update:
                 |${throwable.show}""".stripMargin)
          case Right(_) =>
        }
        ZIO.unit
    }.map(unify)

}