package dev.shvimas.garcon.database.model

import dev.shvimas.telegram.model.Chat
import dev.shvimas.translate.LanguageDirection

case class UserData(chatId: Chat.Id,
                    languageDirection: Option[LanguageDirection],
                    decapitalization: Option[Boolean])
