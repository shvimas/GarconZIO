package dev.shvimas.garcon.model

import java.nio.charset.StandardCharsets

import dev.shvimas.telegram.model.CallbackQuery
import scalapb.GeneratedMessage

object CallbackDataHelper {
  private val charset = StandardCharsets.UTF_8

  def fromString(s: CallbackQuery.Data): Array[Byte] = s.value.getBytes(charset)

  def toString(msg: GeneratedMessage): CallbackQuery.Data = CallbackQuery.Data(msg.toByteString.toString(charset))
}
