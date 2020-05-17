package dev.shvimas.garcon.testing

import dev.shvimas.garcon.Database
import dev.shvimas.garcon.database.DatabaseOps
import dev.shvimas.garcon.database.model.CommonTranslation
import dev.shvimas.garcon.misc.SafeRandom
import dev.shvimas.garcon.testing.TestingBucket.NotEnoughWords
import dev.shvimas.garcon.testing.TestingService.Guess
import dev.shvimas.telegram.model.Chat
import dev.shvimas.translate.LanguageDirection
import zio.{IO, ZIO}

class TestingService private[testing] (testingBucket: TestingBucket) {

  def nextTranslation: ZIO[SafeRandom, Throwable, CommonTranslation] =
    testingBucket.nextWord.mapError {
      case TestingBucket.Unrecoverable(msg)      => new RuntimeException(msg)
      case TestingBucket.RuntimeError(throwable) => throwable
    }

  def update(guess: Guess): IO[TestingBucket.UnexpectedWord, TestingBucket.UpdateStatus] =
    testingBucket.update(guess)
}

object TestingService {

  def make(chatId: Chat.Id,
           languageDirection: LanguageDirection,
           policy: Policy,
  ): ZIO[Database, Throwable, Either[String, TestingService]] =
    for {
      // TODO: take exactly as much words as needed
      sample   <- DatabaseOps.getRandomWords(chatId, languageDirection, policy.numWords * 2)
      wordPool <- ZIO.effect(sample.distinct.take(policy.numWords))
      errorOrService <- TestingBucket
        .make(wordPool, policy.numSuccessesToDrop, initialWeight = policy.initialWeight)
        .commit
        .flatMap(bucket => ZIO.effect(new TestingService(bucket)))
        .mapError { case NotEnoughWords(total) => s"failed to create testing bucket with only $total words" }
        .either
    } yield errorOrService

  case class NoWordsSaved(languageDirection: LanguageDirection)
      extends Exception(s"found no words saved for language direction=$languageDirection")

  case class Policy(numWords: Int, numSuccessesToDrop: Int, initialWeight: Int)

  sealed trait Guess {
    val translation: CommonTranslation
  }
  final case class Success(override val translation: CommonTranslation) extends Guess
  final case class Failure(override val translation: CommonTranslation) extends Guess
}
