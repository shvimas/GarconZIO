package dev.shvimas.garcon.database.model

import dev.shvimas.translate.Translation

case class CommonTranslation(text: String,
                             abbyy: Option[String],
                             yandex: Option[String],
                            ) extends Translation {
  override val originalText: String = text

  override val translatedText: String =
    List(abbyy, yandex)
      .reduce(combineOpts)
      .getOrElse("<no translation>")

  private def combineOpts(firstOpt: Option[String], secondOpt: Option[String]): Option[String] =
    firstOpt -> secondOpt match {
      case (Some(first), Some(second)) => Some(s"$first; $second")
      case (Some(_), None) => firstOpt
      case (None, Some(_)) => secondOpt
      case (None, None) => None
    }
}

object CommonTranslation {
  def from(translation: Translation, translatorName: String): Option[CommonTranslation] =
    translatorName match {
      case CommonTranslationFields.abbyy =>
        Some(CommonTranslation(
          text = translation.originalText,
          abbyy = Some(translation.translatedText),
          yandex = None))

      case CommonTranslationFields.yandex =>
        Some(CommonTranslation(
          text = translation.originalText,
          abbyy = None,
          yandex = Some(translation.translatedText)))

      case _ => None
    }
}