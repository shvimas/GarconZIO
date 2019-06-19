package dev.shvimas.telegram.model

case class Message(messageId: Int,
                   from: Option[User],
                   date: Int,
                   chat: Chat,
                   editDate: Option[Int],
                   text: Option[String],
                   entities: List[MessageEntity],
                   captionEntities: List[MessageEntity])
