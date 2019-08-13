package dev.shvimas.garcon

import cats.syntax.show._
import com.typesafe.scalalogging.LazyLogging
import dev.shvimas.garcon.database.Database
import dev.shvimas.garcon.model._
import dev.shvimas.garcon.utils.ExceptionUtils.showThrowable
import dev.shvimas.telegram._
import dev.shvimas.telegram.model.{Message, Update}
import dev.shvimas.telegram.model.Result.{GetUpdatesResult, SendMessageResult}
import dev.shvimas.translate.LanguageDirection
import scalaz.zio.{ZIO, _}
import scalaz.zio.clock.Clock
import scalaz.zio.duration._

object Main extends App with LazyLogging {

  import BotInteraction._
  import CommonUtils._
  import DatabaseInteraction._
  import MainConfig._
  import TranslatorsInteraction._

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

  def processTranslationRequest(text: String,
                                message: Message,
                               ): ZIO[Database with Translators, Throwable, TranslationResponse] =
    resolveLangDirection(message.chat.id)
      .map(_.maybeReverse(text))
      .flatMap(languageDirection =>
        commonTranslation(text, languageDirection)
          .map(TranslationWithInfo(_, languageDirection, message.messageId)))
      .map(TranslationResponse)

  def processDeleteCommand(command: DeleteCommand,
                           message: Message,
                          ): ZIO[Database, Throwable, DeletionResponse] = {
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

    val chatId = message.chat.id
    val result: ZIO[Database, Throwable, Either[String, String]] =
      command match {
        case DeleteByReply(reply) =>
          reply.text match {
            case Some(text) =>
              ZIO.accessM[Database](_.findLanguageDirectionForMessage(chatId, text, reply.messageId))
                .flatMap {
                  case Some(languageDirection) =>
                    deleteText(text, languageDirection, chatId)
                  case None =>
                    ZIO.succeed(Left(s"Couldn't delete $text (failed to find language direction)"))
                }
            case None =>
              ZIO.succeed(Left("Couldn't delete (text is empty)"))
          }
        case DeleteByText(text, languageDirection) =>
          deleteText(text, languageDirection, chatId)
      }
    result.map(DeletionResponse)
  }

  case class ErrorWithInfo(error: Throwable, update: Update)

  def processUpdate(update: Update): ZIO[Database with Translators, Nothing, Either[ErrorWithInfo, Response]] =
    update.message match {
      case Some(message) =>
        RequestParser.parse(message) match {
          case TranslationRequest(text) =>
            processTranslationRequest(text, message)
              .mapError(ErrorWithInfo(_, update))
              .either
          case deleteCommand: DeleteCommand =>
            processDeleteCommand(deleteCommand, message)
              .mapError(ErrorWithInfo(_, update))
              .either
          case MalformedCommand(desc) =>
            ZIO.succeed(Right(MalformedCommandResponse(desc)))
          case UnrecognisedCommand(command) =>
            ZIO.succeed(Right(UnrecognisedCommandResponse(command)))
          case EmptyRequest =>
            ZIO.succeed(Right(EmptyMessageResponse))
        }
      case None =>
        ZIO.succeed(Right(EmptyMessageResponse))
    }

  type AllResults = List[(Int, List[Either[ErrorWithInfo, Response]])]

  def processGroupedUpdates(updateGroups: Map[Int, Seq[Update]],
                           ): ZIO[Database with Translators, Nothing, AllResults] =
  // important that error type is Nothing in processUpdate
  // otherwise collectAllPar could interrupt other users' processing
    ZIO.collectAllPar(
      updateGroups.map { case (chatId, updates) =>
        ZIO.collectAll(updates.map(processUpdate))
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