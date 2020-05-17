package dev.shvimas.garcon.testing

import dev.shvimas.garcon.database.model.CommonTranslation
import dev.shvimas.garcon.misc.SafeRandom
import dev.shvimas.garcon.testing.TestingBucket._
import dev.shvimas.garcon.testing.TestingService._
import zio.stm._
import zio.{IO, ZIO}

class TestingBucket(perWordStats: TMap[CommonTranslation, Stats], numSuccessesToDrop: Int) {

  def nextWord: ZIO[SafeRandom, NextWordError, CommonTranslation] = {
    def getRandomWord(words: Seq[CommonTranslation]): ZIO[SafeRandom, NextWordError, CommonTranslation] =
      for {
        index <- SafeRandom
          .nextIntBounded(words.length)
          .mapError(err => Unrecoverable(s"negative bound (${err.negativeValue}) for SafeRandom.nextIntBounded"))
        word <- ZIO.effect(words(index)).mapError(RuntimeError)
      } yield word

    for {
      // 'statsList' is an immutable view of `perWordStats` at the moment of the commit
      // therefore it is fine to use it outside of a transaction
      statsList <- perWordStats.toList.commit
      words = statsList.flatMap { case (word, stats) => Seq.fill(stats.weight)(word) }
      word <- getRandomWord(words)
    } yield word
  }

  def update(guess: Guess): IO[UnexpectedWord, UpdateStatus] = {
    val translation = guess.translation
    val stmUpdateStatus: STM[UnexpectedWord, UpdateStatus] = for {
      stats  <- perWordStats.get(translation).flatMap(STM.fromOption).orElseFail(UnexpectedWord(translation.text))
      _      <- updateInnerState(stats.update(guess), translation)
      status <- STM.ifM(perWordStats.size.map(_ < 3))(STM.succeed(NeedRebuild), STM.succeed(Ok))
    } yield status
    stmUpdateStatus.commit
  }

  private def updateInnerState(stats: Stats, translation: CommonTranslation): USTM[Unit] =
    if (stats.weight <= 0 || stats.totalSuccesses >= numSuccessesToDrop)
      perWordStats.delete(translation)
    else
      perWordStats.put(translation, stats)
}

object TestingBucket {
  sealed trait UpdateStatus
  case object Ok          extends UpdateStatus
  case object NeedRebuild extends UpdateStatus

  sealed trait NextWordError
  case class Unrecoverable(msg: String)         extends NextWordError
  case class RuntimeError(throwable: Throwable) extends NextWordError

  case class UnexpectedWord(text: String)

  case class Stats(weight: Int, totalSuccesses: Int) {

    def update(guess: Guess): Stats = guess match {
      case _: Success => Stats(weight = weight - 1, totalSuccesses = totalSuccesses + 1)
      case _: Failure => Stats(weight = weight + 1, totalSuccesses = totalSuccesses)
    }
  }

  case class NotEnoughWords(total: Int)

  def make(translations: Seq[CommonTranslation],
           numSuccessesToDrop: Int,
           initialWeight: Int): STM[NotEnoughWords, TestingBucket] =
    for {
      _            <- STM.unless(translations.nonEmpty)(STM.fail(NotEnoughWords(translations.length)))
      initialStats <- TMap.make(translations.map(_ -> Stats(weight = initialWeight, totalSuccesses = 0)): _*)
    } yield new TestingBucket(initialStats, numSuccessesToDrop)
}
