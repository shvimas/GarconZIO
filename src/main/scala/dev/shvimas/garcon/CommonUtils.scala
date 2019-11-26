package dev.shvimas.garcon

import com.typesafe.scalalogging.LazyLogging

object CommonUtils extends LazyLogging {
  def unify(units: (Unit, Unit)): Unit = ()

  def unify[T](unit: Unit, t: T): T = t

  def unify(units: Seq[Unit]): Unit = ()

  def unify(units: Either[Unit, Unit]): Unit = ()

  def logErrorWithContext(contextWithThrowable: (String, Throwable)): Unit = {
    val (context, throwable) = contextWithThrowable
    logErrorWithContext(context, throwable)
  }

  def logErrorWithContext(context: String, throwable: Throwable): Unit =
    logger.error(context, throwable)
}
