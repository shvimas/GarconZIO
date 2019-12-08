package dev.shvimas.garcon

import dev.shvimas.garcon.Main._
import dev.shvimas.garcon.database.model.CommonTranslation
import dev.shvimas.garcon.model._
import dev.shvimas.telegram.Bot
import dev.shvimas.telegram.model.{InlineKeyboardButton, InlineKeyboardMarkup}
import dev.shvimas.telegram.model.Result.SendMessageResult
import dev.shvimas.translate.LanguageDirection
import zio.ZIO

object BotInteraction {

  type SendResponsesResults = List[(Int, Either[Throwable, List[SendMessageResult]])]

  def sendResponses(results: AllResults): ZIO[Bot, Nothing, SendResponsesResults] =
    ZIO.collectAllPar(
        results.map { case (chatId, resultsPerUser) => sendPerUsersResponses(chatId, resultsPerUser) }
    )

  private def sendPerUsersResponses(
      chatId: Int,
      resultsPerUser: List[Either[ErrorWithInfo, Response]]
  ): ZIO[Bot, Nothing, (Int, Either[Throwable, List[SendMessageResult]])] =
    ZIO
      .collectAll(
          resultsPerUser.map { errorOrResult: Either[ErrorWithInfo, Response] =>
            {
              var replyMarkup: Option[InlineKeyboardMarkup] = None
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
                    case testResponse: TestResponse =>
                      def makeShowButton(translation: CommonTranslation,
                                         languageDirection: LanguageDirection,
                      ): InlineKeyboardButton = {
                        val data = TestShowResponse
                          .makeCallbackData(languageDirection, translation.text)
                        InlineKeyboardButton("Show", data)
                      }

                      def makeNextButton(
                          languageDirection: LanguageDirection
                      ): InlineKeyboardButton = {
                        val data = TestNextResponse.makeCallbackData(languageDirection)
                        InlineKeyboardButton("Next", data)
                      }

                      def makeReplyMarkup(translation: CommonTranslation,
                                          languageDirection: LanguageDirection,
                      ): InlineKeyboardMarkup =
                        InlineKeyboardMarkup.makeRow(
                            makeShowButton(translation, languageDirection),
                            makeNextButton(languageDirection)
                        )

                      testResponse match {
                        case TestStartResponse(Some(translation), languageDirection) =>
                          replyMarkup = Some(makeReplyMarkup(translation, languageDirection))
                          translation.text
                        case TestStartResponse(None, languageDirection) =>
                          s"Seems like you have no words saved for $languageDirection"
                        case TestNextResponse(Some(translation), languageDirection) =>
                          replyMarkup = Some(makeReplyMarkup(translation, languageDirection))
                          translation.text
                        case TestNextResponse(None, languageDirection) =>
                          s"Seems like you have no words saved for $languageDirection"
                        case TestShowResponse(maybeTranslation, languageDirection) =>
                          replyMarkup = Some(
                              InlineKeyboardMarkup.makeRow(makeNextButton(languageDirection))
                          )
                          maybeTranslation match {
                            case Some(translation) =>
                              translation.translatedText
                            case None =>
                              s"Seems like no translation is saved 😔"
                          }
                      }
                    case SuccessfulChooseResponse(languageDirection) =>
                      s"Switched language direction to $languageDirection"
                    case FailedChooseResponse(desc, languageDirection) =>
                      s"Error: $desc while switching to $languageDirection"
                    case DecapitalizeResponse(state) =>
                      s"Decapitalization is $state"
                    case HelpResponse =>
                      HelpResponse.message
                    case MalformedCommandResponse(desc) =>
                      desc
                    case UnrecognisedCommandResponse(command) =>
                      s"Can't understand your command ($command)"
                    case EmptyUpdateResponse =>
                      "Got an empty update"
                    case EmptyMessageResponse =>
                      "Got an empty message"
                    case EmptyCallbackDataResponse =>
                      "Got an empty callback data"
                  }
              }
              sendMessage(chatId, response, replyMarkup)
            }
          }
      )
      .either
      .map(chatId -> _)

  def sendMessage(chatId: Int,
                  response: String,
                  replyMarkup: Option[InlineKeyboardMarkup],
  ): ZIO[Bot, Throwable, SendMessageResult] =
    for {
      triedResult <- ZIO.access[Bot](_.sendMessage(chatId, Some(response), replyMarkup = replyMarkup))
      result      <- ZIO.fromTry(triedResult)
    } yield result
}
