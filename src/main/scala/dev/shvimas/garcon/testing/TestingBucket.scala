package dev.shvimas.garcon.testing

import dev.shvimas.garcon.database.model.CommonTranslation

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/**
  * Not intended to be thread-safe.
  * <p>
  * Uses a simple strategy: starts with all words' probabilities distributed evenly
  * and increases it when user fails to translate.
  * If user successfully translates the word then probability should decrease but never reach zero.
  * A word should be removed once it is guessed `numSuccessesToDrop` times in a row.
  * */
class TestingBucket(translations: Seq[CommonTranslation],
                    numSuccessesToDrop: Int) {
  private[testing] val translationsWithRepeats: ArrayBuffer[CommonTranslation] =
    new ArrayBuffer[CommonTranslation](translations.length * 2) ++= translations

  private[testing] val initialPositions =
    mutable.Map(translations.zipWithIndex: _*)

  private[testing] val stats: mutable.Map[CommonTranslation, TestingStats] = {
    val elems = translations zip Stream.continually(TestingStats.empty)
    mutable.Map(elems: _*)
  }

  private val rnd = new Random

  def randomTranslation: CommonTranslation = {
    val index = rnd.nextInt(translationsWithRepeats.length)
    translationsWithRepeats(index)
  }

  def addStats(translation: CommonTranslation, guessed: Boolean): Unit = {
    require(stats contains translation, s"unexpected translation: $translation")
    val curStats = stats(translation).increment(guessed)
    if (guessed) {
      if (curStats.successes >= numSuccessesToDrop) {
        translationsWithRepeats.remove(initialPositions(translation))
        initialPositions.remove(translation)
        stats.remove(translation)
      } else {
        curStats.indices.headOption.foreach(translationsWithRepeats.remove)
        val updatedStats = curStats.dropLastIndex
        stats.update(translation, updatedStats)
      }
    } else {
      translationsWithRepeats += translation
      val updatedStats = curStats.addIndex(translationsWithRepeats.length - 1)
      stats.update(translation, updatedStats)
    }
  }

}
