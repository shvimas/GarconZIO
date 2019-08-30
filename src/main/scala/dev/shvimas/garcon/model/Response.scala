package dev.shvimas.garcon.model

import dev.shvimas.garcon.database.model.CommonTranslation
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
    s"test next $languageDirection"
  }
}

case class TestShowResponse(maybeTranslation: Option[CommonTranslation],
                            languageDirection: LanguageDirection,
                           ) extends TestResponse

sealed trait ErrorResponse extends Response

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
      |Please contact @shvimas if you have any follow-up questions.""".stripMargin
}

case class MalformedCommandResponse(desc: String) extends ErrorResponse

case class UnrecognisedCommandResponse(command: String) extends ErrorResponse

object EmptyUpdateResponse extends ErrorResponse

object EmptyCallbackDataResponse extends ErrorResponse

object EmptyMessageResponse extends ErrorResponse

case class TranslationWithInfo(translation: CommonTranslation,
                               languageDirection: LanguageDirection,
                               messageId: Int)
