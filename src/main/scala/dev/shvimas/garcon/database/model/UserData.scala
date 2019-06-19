package dev.shvimas.garcon.database.model

import dev.shvimas.translate.LanguageDirection

case class UserData(chatId: Int,
                    languageDirection: Option[LanguageDirection],
                    translator: Option[String])
