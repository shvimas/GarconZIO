package dev.shvimas.garcon.database.model

import dev.shvimas.translate.Translation

case class CommonTranslation(text: String,
                             translations: Map[String, String]) extends Translation {
  override val originalText: String = text

  override val translatedText: String =
    translations
      .map { case (name, translation) => s"$name: $translation" }
      .reduceOption((left, right) => s"$left\n$right")
      .getOrElse("<no translation>")

  def isEmpty: Boolean = translations.isEmpty
}