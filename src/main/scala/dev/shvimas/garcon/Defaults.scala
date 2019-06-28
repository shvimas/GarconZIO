package dev.shvimas.garcon

import dev.shvimas.garcon.database.model.{CommonTranslationFields, UserData}
import dev.shvimas.translate.LanguageDirection

object Defaults {
  val languageDirection: LanguageDirection = LanguageDirection.EN_RU

  val translatorName: String = CommonTranslationFields.abbyy

  def userData(chatId: Int): UserData =
    UserData(chatId = chatId, languageDirection = Some(languageDirection))
}
