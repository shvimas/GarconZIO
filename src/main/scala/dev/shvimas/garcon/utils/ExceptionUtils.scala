package dev.shvimas.garcon.utils

import java.io.{PrintWriter, StringWriter}

import cats.Show

object ExceptionUtils {
  implicit val showThrowable: Show[Throwable] =
    (t: Throwable) => {
      val sw = new StringWriter
      t.printStackTrace(new PrintWriter(sw))
      sw.toString
    }
}
