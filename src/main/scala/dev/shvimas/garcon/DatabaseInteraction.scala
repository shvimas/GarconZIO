package dev.shvimas.garcon

import cats.syntax.show._
import com.typesafe.scalalogging.StrictLogging
import dev.shvimas.garcon.database.Database
import dev.shvimas.garcon.utils.ExceptionUtils.showThrowable
import dev.shvimas.garcon.Main.AllResults
import dev.shvimas.garcon.database.response.UpdateResult
import dev.shvimas.telegram.model.Result.GetUpdatesResult
import dev.shvimas.translate.LanguageDirection
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
        .map(updateResult => require(updateResult.modifiedCount == 1))
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
      .map {
        case Some(userData) => userData.languageDirection.getOrElse(Defaults.languageDirection)
        case None => Defaults.languageDirection
      }

  def saveResults(allResults: AllResults): ZIO[Database, Nothing, Unit] =
    ZIO.collectAllPar(
      allResults.flatMap { case (chatId, perUserResults) =>
        perUserResults.map {
          case Left(_) => ZIO.unit
          case Right(None) => ZIO.unit
          case Right(Some((translation, name, languageDirection))) =>
            ZIO.accessM[Database](_.addText(translation, name, chatId -> languageDirection))
              .map((updateResult: UpdateResult) =>
                if (!updateResult.wasAcknowledged) {
                  logger.error(s"Tried to save $translation for $chatId but it was not acknowledged")
                })
              .either
              .map {
                case Left(throwable) => logger.error(
                  s"""While saving $translation for $chatId:
                     |${throwable.show}""".stripMargin)
                case Right(()) =>
              }
        }
      }
    ).map(unify)

}
