package dev.shvimas.garcon.database.model

import dev.shvimas.translate.Translation

case class CommonTranslation(text: String, translations: Map[String, String], edited: Option[String])
    extends Translation {
  override val originalText: String = text

  override val translatedText: String = {
    val translationsAsString =
      translations
        .map { case (name, translation) => s"$name: $translation" }
        .reduceOption((left, right) => s"$left\n$right")
        .getOrElse("<no translation>")
    edited match {
      case Some(value) =>
        s"""$translationsAsString
           |edited: $value""".stripMargin
      case None => translationsAsString
    }
  }

  def isEmpty: Boolean = translations.isEmpty

  def nonEmpty: Boolean = translations.nonEmpty

  def mergeWith(other: CommonTranslation): CommonTranslation =
    copy(edited = edited.orElse(other.edited))
}
