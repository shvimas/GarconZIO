package dev.shvimas.garcon.testing

import dev.shvimas.garcon.testing.TestingService.Guess

case class TestingStats(tries: Int, successes: Int, indices: List[Int]) {
  def increment(guess: Guess): TestingStats = {
    guess match {
      case TestingService.Success(_) => copy(successes = successes + 1, tries = tries + 1)
      case TestingService.Failure(_) => copy(tries = tries + 1)
    }
  }

  def addIndex(index: Int): TestingStats =
    copy(indices = index :: indices)

  def dropLastIndex: TestingStats = {
    val newIndices = indices match {
      case Nil       => Nil
      case _ :: tail => tail
    }
    copy(indices = newIndices)
  }
}

object TestingStats {
  def empty: TestingStats =
    new TestingStats(tries = 0, successes = 0, indices = Nil)
}
