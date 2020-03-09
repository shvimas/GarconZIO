package dev.shvimas.garcon

import com.typesafe.scalalogging.LazyLogging

object CommonUtils extends LazyLogging {
  def logErrorWithContext(contextWithThrowable: (String, Throwable)): Unit = {
    val (context, throwable) = contextWithThrowable
    logErrorWithContext(context, throwable)
  }

  def logErrorWithContext(context: String, throwable: Throwable): Unit =
    logger.error(context, throwable)
}
