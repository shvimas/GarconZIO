package dev.shvimas.garcon.testing

case class TestingStats(tries: Int, successes: Int, indices: List[Int]) {
  def increment(success: Boolean): TestingStats = {
    copy(successes = successes + (if (success) 1 else 0))
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
