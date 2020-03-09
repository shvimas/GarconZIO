package dev.shvimas.telegram.model

import dev.shvimas.telegram.model.Update._

case class Update(updateId: Id, message: Option[Message], callbackQuery: Option[CallbackQuery]) {

  def chatId: Option[Chat.Id] = {
    message.foreach(msg => return Some(msg.chat.id))
    callbackQuery.foreach(cb => return Some(cb.from.id))
    None
  }
}

object Update {
  case class Id(value: Long) extends AnyVal

  object Id {
    def max(id1: Id, id2: Id): Id = if (id1.value >= id2.value) id1 else id2
  }

}
