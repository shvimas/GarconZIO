package dev.shvimas

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import zio.{UIO, ZIO}

trait ZIOLogging {
  protected val zioLogger = new ZIOLogger
}

class ZIOLogger {
  private val logger = Logger(LoggerFactory.getLogger(getClass.getName))

  private def wrap(action: => Unit): UIO[Unit] = ZIO.effect(action).fold(_ => (), _ => ())

  def error(message: String): UIO[Unit]                   = wrap(logger.error(message))
  def error(message: String, cause: Throwable): UIO[Unit] = wrap(logger.error(message, cause))

  def warn(message: String): UIO[Unit]                   = wrap(logger.warn(message))
  def warn(message: String, cause: Throwable): UIO[Unit] = wrap(logger.warn(message, cause))

  def info(message: String): UIO[Unit]                   = wrap(logger.info(message))
  def info(message: String, cause: Throwable): UIO[Unit] = wrap(logger.info(message, cause))

  def debug(message: String): UIO[Unit]                   = wrap(logger.debug(message))
  def debug(message: String, cause: Throwable): UIO[Unit] = wrap(logger.debug(message, cause))

  def trace(message: String): UIO[Unit]                   = wrap(logger.trace(message))
  def trace(message: String, cause: Throwable): UIO[Unit] = wrap(logger.trace(message, cause))
}
