package dev.shvimas.telegram

package object model {
  type GetMeResult       = Result[User]
  type GetUpdatesResult  = Result[List[Update]]
  type SendMessageResult = Result[Message]
  type BooleanResult     = Result[Boolean]
}
