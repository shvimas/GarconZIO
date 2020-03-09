package dev.shvimas.garcon

import dev.shvimas.garcon.database.model.{CommonTranslationFields, UserData}
import dev.shvimas.telegram.model.Chat
import dev.shvimas.translate.LanguageDirection

object Defaults {
  val languageDirection: LanguageDirection = LanguageDirection.EN_RU

  val translatorName: String = CommonTranslationFields.abbyy

  val decapitalization: Boolean = true

  def userData(chatId: Chat.Id,
               languageDirection: LanguageDirection = Defaults.languageDirection,
               decapitalization: Boolean = Defaults.decapitalization,
  ): UserData =
    UserData(
        chatId = chatId,
        languageDirection = Some(languageDirection),
        decapitalization = Some(decapitalization),
    )
}
