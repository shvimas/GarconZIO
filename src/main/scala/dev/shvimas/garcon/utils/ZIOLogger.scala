package dev.shvimas.garcon.utils

import com.typesafe.scalalogging.LazyLogging
import scalaz.zio.{UIO, ZIO}

import scala.util.Try

object ZIOLogger extends LazyLogging {
  private def wrap(action: => Unit): UIO[Unit] =
    ZIO.effectTotal(Try(action).recoverWith { case t => Try(logger.error("", t)) })

  def error(msg: String): UIO[Unit] =
    wrap(logger.error(msg))

  def error(msg: String, cause: Throwable): UIO[Unit] =
    wrap(logger.error(msg, cause))

  def info(msg: String): UIO[Unit] =
    wrap(logger.info(msg))

  def info(msg: String, cause: Throwable): UIO[Unit] =
    wrap(logger.info(msg, cause))

  def infoLines(msgs: String*): UIO[Unit] =
    wrap(logger.info(msgs.mkString("\n")))

  def warn(msg: String): UIO[Unit] =
    wrap(logger.warn(msg))

  def warn(msg: String, cause: Throwable): UIO[Unit] =
    wrap(logger.warn(msg, cause))

  def warnLines(msgs: String*): UIO[Unit] =
    wrap(logger.warn(msgs.mkString("\n")))

  def debug(msg: String): UIO[Unit] =
    wrap(logger.debug(msg))

  def debug(msg: String, cause: Throwable): UIO[Unit] =
    wrap(logger.debug(msg, cause))

  def trace(msg: String): UIO[Unit] =
    wrap(logger.trace(msg))

  def trace(msg: String, cause: Throwable): UIO[Unit] =
    wrap(logger.trace(msg, cause))
}
