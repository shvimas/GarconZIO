package dev.shvimas.garcon

import dev.shvimas.garcon.Main._
import dev.shvimas.garcon.database.model.CommonTranslation
import dev.shvimas.garcon.model._
import dev.shvimas.telegram.Bot
import dev.shvimas.telegram.model._
import dev.shvimas.translate.LanguageDirection
import dev.shvimas.ZIOLogging
import zio.{Has, ZIO}

object BotInteraction extends ZIOLogging {

  case class Reply(text: String, markup: Option[InlineKeyboardMarkup])

  type PerUserSendResult = Either[Throwable, List[SendMessageResult]]

  def sendResponses(results: AllResults): ZIO[Has[Bot], Nothing, List[(Chat.Id, PerUserSendResult)]] =
    ZIO.foreachPar(results) { case (chatId, resultsPerUser) => sendPerUsersResponses(chatId, resultsPerUser) }

  private def sendPerUsersResponses(
      chatId: Chat.Id,
      resultsPerUser: List[Either[ErrorWithInfo, Response]]
  ): ZIO[Has[Bot], Nothing, (Chat.Id, PerUserSendResult)] =
    ZIO
      .foreach(resultsPerUser) { result: Either[ErrorWithInfo, Response] =>
        for {
          reply <- ZIO.effect(makeReply(result))
          text = Some(reply.text)
          sendMessageResult <- BotOps.sendMessage(chatId, text, reply.markup, disableNotification = true)
        } yield sendMessageResult
      }
      .logOnError(s"Failed to send response(s): $resultsPerUser")
      .either
      .map(chatId -> _)

  private def makeReply(errorOrResult: Either[ErrorWithInfo, Response]): Reply = {
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
              val data = TestShowResponse.makeCallbackData(languageDirection, translation.text)
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
          case SuccessfulEditResponse(text, languageDirection, edit) =>
            s"Edited $text ($languageDirection): $edit"
          case FailedEditResponse(desc) =>
            desc
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
          case BothMessageAndCallbackResponse(update) =>
            s"""Got an update with
               |  message ${update.message}
               |  callback query ${update.callbackQuery}""".stripMargin
        }
    }
    Reply(response, replyMarkup)
  }

}
