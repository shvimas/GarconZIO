package dev.shvimas.translate.abbyy

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import dev.shvimas.translate.Translation

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
  private val codec: JsonValueCodec[AbbyyTranslation] =
    JsonCodecMaker.make(
        CodecMakerConfig
          .withFieldNameMapper(JsonCodecMaker.EnforcePascalCase)
    )

  def fromJson(bytes: Array[Byte]): Try[AbbyyTranslation] =
    Try(readFromArray(bytes)(codec))
}

case class WordListItem(
    heading: String,
    translation: String,
    dictionaryName: String,
    soundName: String,
    originalWord: String
)
