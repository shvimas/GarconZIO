package dev.shvimas.garcon

import dev.shvimas.garcon.Main._
import dev.shvimas.garcon.model._
import dev.shvimas.telegram.Bot
import dev.shvimas.telegram.model.Result.SendMessageResult
import scalaz.zio.ZIO

object BotInteraction {

  type SendResponsesResults = List[(Int, Either[Throwable, List[SendMessageResult]])]

  def sendResponses(results: AllResults): ZIO[Bot, Nothing, SendResponsesResults] =
    ZIO.collectAllPar(
      results.map { case (chatId: Int, resultsPerUser: List[Either[ErrorWithInfo, Response]]) =>
        ZIO.collectAll(
          resultsPerUser.map { errorOrResult: Either[ErrorWithInfo, Response] => {
            val response: String = errorOrResult match {
              case Left(ErrorWithInfo(throwable, _)) =>
                throwable.toString // give small error output
              case Right(result) =>
                result match {
                  case TranslationResponse(translationWithInfo) =>
                    translationWithInfo.translation.translatedText
                  case DeletionResponse(Left(error)) =>
                    s"Error: $error"
                  case DeletionResponse(Right(text)) =>
                    text
                  case UnrecognisedCommandResponse(command) =>
                    s"Can't understand your command ($command)"
                  case EmptyMessageResponse =>
                    "Got empty message"
                }
            }
            sendMessage(chatId, response)
          }
          }
        ).either.map(chatId -> _)
      }
    )

  def sendMessage(chatId: Int,
                  response: String,
                 ): ZIO[Bot, Throwable, SendMessageResult] =
    ZIO.access[Bot](_.sendMessage(chatId, Some(response)))
      .flatMap(t => ZIO.fromTry(t))
}
