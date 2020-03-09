package dev.shvimas.telegram.model

import dev.shvimas.telegram.model.CallbackQuery._

case class CallbackQuery(id: Id,
                         from: User,
                         message: Option[Message],
                         inlineMessageId: Option[InlineMessageId],
                         data: Option[Data])

object CallbackQuery {
  case class Id(value: String)              extends AnyVal
  case class Data(value: String)            extends AnyVal
  case class InlineMessageId(value: String) extends AnyVal
}
