package dev.shvimas.garcon

import dev.shvimas.garcon.Main.{AllResults, Result}
import dev.shvimas.telegram.Bot
import dev.shvimas.telegram.model.Result.SendMessageResult
import scalaz.zio.ZIO

object BotInteraction {

  type SendResponsesResults = List[(Int, Either[Throwable, List[SendMessageResult]])]

  def sendResponses(results: AllResults): ZIO[Bot, Nothing, SendResponsesResults] =
    ZIO.collectAllPar(
      results.map { case (chatId: Int, resultsPerUser: List[Result]) =>
        ZIO.collectAll(
          resultsPerUser.map { result: Result => {
            val response: Option[String] = result match {
              case Left((throwable, _)) => Some(throwable.toString) // give small error output
              case Right(None) => None
              case Right(Some(maybeTranslationWithInfo)) => Some(maybeTranslationWithInfo._1.translatedText)
            }
            sendMessage(chatId, response)
          }
          }
        ).either.map(chatId -> _)
      }
    )

  def sendMessage(chatId: Int,
                  response: Option[String],
                 ): ZIO[Bot, Throwable, SendMessageResult] =
    ZIO.access[Bot](_.sendMessage(chatId, response))
      .flatMap(t => ZIO.fromTry(t))
}
