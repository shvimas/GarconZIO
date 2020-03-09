package dev.shvimas.telegram.model

import dev.shvimas.telegram.model.Message._

case class Message(messageId: Id,
                   from: Option[User],
                   date: Date,
                   chat: Chat,
                   editDate: Option[EditDate],
                   text: Option[Text],
                   entities: List[MessageEntity],
                   captionEntities: List[MessageEntity],
                   replyToMessage: Option[Message],
                   replyMarkup: Option[InlineKeyboardMarkup])

object Message {
  case class Id(value: Long)       extends AnyVal
  case class Date(value: Long)     extends AnyVal
  case class EditDate(value: Long) extends AnyVal
  case class Text(value: String)   extends AnyVal
}
