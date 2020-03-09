package dev.shvimas.telegram.model

case class Result[A](ok: Boolean, result: A)
