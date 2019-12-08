package dev.shvimas.garcon.model

import dev.shvimas.garcon.database.model.CommonTranslation
import dev.shvimas.garcon.model.proto.callback_data.{CallbackRequest, TestNextData, TestShowData}
import dev.shvimas.telegram.model.Update
import dev.shvimas.translate.LanguageDirection

sealed trait Response

case class TranslationResponse(translationWithInfo: TranslationWithInfo) extends Response

case class DeletionResponse(response: Either[String, String]) extends Response

sealed trait TestResponse extends Response

case class TestStartResponse(maybeTranslation: Option[CommonTranslation],
                             languageDirection: LanguageDirection,
) extends TestResponse

case class TestNextResponse(maybeTranslation: Option[CommonTranslation],
                            languageDirection: LanguageDirection,
) extends TestResponse

object TestNextResponse {

  def makeCallbackData(languageDirection: LanguageDirection): String = {
    val data = TestNextData(
        langDir = languageDirection.toString,
    )
    CallbackDataHelper.toString(CallbackRequest(data))
  }
}

case class TestShowResponse(maybeTranslation: Option[CommonTranslation],
                            languageDirection: LanguageDirection,
) extends TestResponse

object TestShowResponse {

  def makeCallbackData(languageDirection: LanguageDirection, text: String): String = {
    val data = TestShowData(
        langDir = languageDirection.toString,
        text = text,
    )
    CallbackDataHelper.toString(CallbackRequest(data))
  }
}

sealed trait ChooseResponse extends Response

case class SuccessfulChooseResponse(languageDirection: LanguageDirection) extends ChooseResponse

case class FailedChooseResponse(description: String, languageDirection: LanguageDirection)
    extends ChooseResponse

case class DecapitalizeResponse(state: DecapitalizeCommand.State.Value) extends Response

object HelpResponse extends Response {

  val message: String =
    """Hi! I'm Garcon, your personal vocabulary trainer.
      |Just send me anything and I'll translate it for you!
      |Supported commands (<...> means parameter):
      | - /help
      |    Show this help
      | - /test <language direction>
      |    Start testing mode
      |    E.g. /test en-ru
      | - /delete <text> <language direction>
      |    Delete text from saved
      |    E.g. /delete ubiquitous inquiry en-ru
      |    It's also possible to reply /delete to your message with text to be deleted
      | - /choose <language direction>
      |    Choose current language direction
      |    E.g. /choose en-ru
      |Please contact @shvimas if you have any follow-up questions.""".stripMargin
}

sealed trait ErrorResponse extends Response

case class MalformedCommandResponse(desc: String) extends ErrorResponse

case class UnrecognisedCommandResponse(command: String) extends ErrorResponse

object EmptyUpdateResponse extends ErrorResponse

object EmptyCallbackDataResponse extends ErrorResponse

object EmptyMessageResponse extends ErrorResponse

case class BothMessageAndCallbackResponse(update: Update) extends ErrorResponse

case class TranslationWithInfo(translation: CommonTranslation,
                               languageDirection: LanguageDirection,
                               messageId: Int)
