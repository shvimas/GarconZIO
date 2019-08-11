package dev.shvimas.garcon.model

import dev.shvimas.garcon.database.model.CommonTranslation
import dev.shvimas.translate.LanguageDirection

sealed trait Response

case class TranslationResponse(translationWithInfo: TranslationWithInfo) extends Response

case class DeletionResponse(response: Either[String, String]) extends Response

case class UnrecognisedCommandResponse(command: String) extends Response

object EmptyMessageResponse extends Response

case class TranslationWithInfo(translation: CommonTranslation,
                               languageDirection: LanguageDirection,
                               messageId: Int)
