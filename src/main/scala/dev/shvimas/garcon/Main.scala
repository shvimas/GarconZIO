package dev.shvimas.garcon

import cats.syntax.show._
import com.typesafe.scalalogging.LazyLogging
import dev.shvimas.garcon.database.Database
import dev.shvimas.garcon.database.model.CommonTranslation
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

  type Result = Either[(Throwable, Update), Option[TranslationWithInfo]]

  def processUpdatesPerUser(chatId: Int,
                            updates: Iterable[Update],
                           ): ZIO[Database with Translators, Nothing, (Int, List[Result])] =
    ZIO.collectAll(
      updates.map(
        update =>
          update.message match {
            case Some(message) =>
              translate(message)
                .mapError(_ -> update)
                .either
            case None =>
              ZIO.succeed(None).either
          }
      )
    ).map(results => chatId -> results)

  type AllResults = List[(Int, List[Result])]

  def processGroupedUpdates(updateGroups: Map[Int, Seq[Update]],
                           ): ZIO[Database with Translators, Nothing, AllResults] =
  // important that error type is Nothing in processUpdatesPerUser
  // otherwise collectAllPar could interrupt other users' processing
    ZIO.collectAllPar(
      updateGroups.map { case (chatId, updates) =>
        processUpdatesPerUser(chatId, updates)
      }
    )

  private def processOrphanUpdates(orphanUpdates: Seq[Update]): Unit = {
    logger.warn(
      s"""Got updates from unknown chat:
         |${orphanUpdates.mkString("\n")}


         |These updates were left unattended""".stripMargin)
  }

  def processErrors(allResults: AllResults): ZIO[Any, Nothing, Unit] =
    ZIO.foreachPar(allResults) {
      case (chatId, resultsPerUser: List[Result]) =>
        resultsPerUser.foreach {
          case Left((throwable: Throwable, update: Update)) =>
            logger.error(
              s"""While processing $update:
                 |${throwable.show}""".stripMargin)
          case Right(None) =>
            logger.warn(s"Got empty translation for chat with id: $chatId")
          case Right(Some(_)) =>
        }
        ZIO.unit
    }.map(unify)


  type TranslationWithInfo = (CommonTranslation, LanguageDirection)

  def translate(message: Message): ZIO[Database with Translators, Throwable, Option[TranslationWithInfo]] =
    ZIO.succeed(message.text).flatMap {
      case Some(text) =>
        resolveLangDirection(message.chat.id)
          .map(_.maybeReverse(text))
          .flatMap(languageDirection =>
            commonTranslation(text, languageDirection)
              .map(_ -> languageDirection)
          ).map(Some(_))
      case None => ZIO.succeed(None)
    }


}
