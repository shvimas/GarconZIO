package dev.shvimas.translate

import dev.shvimas.translate.LanguageCode.LanguageCode

case class LanguageDirection(source: LanguageCode,
                             target: LanguageCode) {
  def maybeReverse(text: String): LanguageDirection =
    if (LanguageGuesser.testLanguage(target, text))
      LanguageDirection(target, source)
    else this

  override def toString: String = s"$source-$target"
}

case object LanguageDirection {

  import LanguageCode._

  val EN_RU = LanguageDirection(EN, RU)
  val RU_EN = LanguageDirection(RU, EN)
}

