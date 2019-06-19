package dev.shvimas.translate.abbyy

import dev.shvimas.translate.Translation
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.JsonMethods.parse

import scala.util.Try

case class AbbyyTranslation(
    sourceLanguage: Int,
    targetLanguage: Int,
    heading: String,
    translation: WordListItem,
    seeAlso: List[String]
) extends Translation {
  override val translatedText: String = translation.translation
  override val originalText: String   = translation.heading
}

object AbbyyTranslation {
  implicit private val formats: Formats = DefaultFormats

  def fromString(string: String): Try[AbbyyTranslation] =
    Try(parse(string).camelizeKeys.extract[AbbyyTranslation])
}

case class WordListItem(
    heading: String,
    translation: String,
    dictionaryName: String,
    soundName: String,
    originalWord: String
)
