package dev.shvimas

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import zio.{UIO, ZIO, ZLayer}

trait ZIOLogging {
  protected val zioLogger = new ZIOLogger(getClass)

  implicit class ZioLoggingOps[R, E <: Throwable, A](zio: ZIO[R, E, A]) {
    def logOnError(message: String): ZIO[R, E, A] = zio.tapError(zioLogger.error(message, _))
  }

  implicit class ZLayerLoggingOps[I, E <: Throwable, O](zLayer: ZLayer[I, E, O]) {
    def logOnError(message: String): ZLayer[I, E, O] = zLayer.tapError(zioLogger.error(message, _))
  }
}

class ZIOLogger(clazz: Class[_]) {
  private val logger = Logger(LoggerFactory.getLogger(clazz))

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
