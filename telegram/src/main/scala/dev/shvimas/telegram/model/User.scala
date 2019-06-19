package dev.shvimas.telegram.model

case class User(id: Int,
                isBot: Boolean,
                firstName: String,
                lastName: Option[String],
                username: Option[String],
                languageCode: Option[String])
