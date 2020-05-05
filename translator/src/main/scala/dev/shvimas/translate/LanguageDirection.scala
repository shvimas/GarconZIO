package dev.shvimas.translate

import dev.shvimas.translate.LanguageCode.LanguageCode

import scala.util.Try

case class LanguageDirection(source: LanguageCode, target: LanguageCode) {

  def maybeReverse(text: String): LanguageDirection =
    if (LanguageGuesser.testLanguage(target, text))
      LanguageDirection(target, source)
    else this

  override def toString: String = s"$source-$target"
}

//noinspection TypeAnnotation
case object LanguageDirection {

  import LanguageCode._

  val EN_RU = LanguageDirection(EN, RU)
  val RU_EN = LanguageDirection(RU, EN)

  private val pattern = "(..)-(..)".r

  def parse(s: String): Option[LanguageDirection] =
    s match {
      case pattern(source, target) =>
        val triedLanguageDirection = for {
          sourceLC <- Try(LanguageCode.withName(source))
          targetLC <- Try(LanguageCode.withName(target))
        } yield LanguageDirection(sourceLC, targetLC)
        triedLanguageDirection.toOption
      case _ => None
    }
}
