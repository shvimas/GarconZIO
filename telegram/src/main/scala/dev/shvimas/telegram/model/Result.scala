package dev.shvimas.telegram.model

case class Result[A](ok: Boolean, result: A)

object Result {
  type GetMeResult       = Result[User]
  type GetUpdatesResult  = Result[List[Update]]
  type SendMessageResult = Result[Message]
  type BooleanResult     = Result[Boolean]
}
