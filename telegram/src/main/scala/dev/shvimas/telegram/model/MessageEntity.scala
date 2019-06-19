package dev.shvimas.telegram.model

case class MessageEntity(`type`: String,
                         offset: Int,
                         length: Int,
                         url: Option[String],
                         user: Option[User])
