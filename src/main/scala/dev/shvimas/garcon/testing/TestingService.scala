package dev.shvimas.garcon.testing

import dev.shvimas.garcon.database.model.CommonTranslation
import dev.shvimas.garcon.testing.TestingService.Guess
import zio.{Task, ZIO}

import scala.util.Random

trait TestingService {
  def nextWord: ZIO[Random, Throwable, CommonTranslation]

  def addGuess(guess: Guess): Task[Unit]
}

object TestingService {
  sealed trait Guess
  final case class Success(translation: CommonTranslation) extends Guess
  final case class Failure(translation: CommonTranslation) extends Guess
}
