package dev.shvimas.garcon.model

import java.nio.charset.StandardCharsets

import scalapb.GeneratedMessage

object CallbackDataHelper {
  private val charset = StandardCharsets.UTF_8

  def fromString(s: String): Array[Byte] = s.getBytes(charset)

  def toString(msg: GeneratedMessage): String = msg.toByteString.toString(charset)
}
