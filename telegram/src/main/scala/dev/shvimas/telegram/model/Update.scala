package dev.shvimas.telegram.model

case class Update(updateId: Int, message: Option[Message], callbackQuery: Option[CallbackQuery]) {

  def chatId: Option[Int] = {
    message.foreach(msg => return Some(msg.chat.id))
    callbackQuery.foreach(cb => return Some(cb.from.id))
    None
  }
}
