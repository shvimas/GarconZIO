package dev.shvimas.translate.yandex

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import dev.shvimas.translate.Translation

import scala.util.Try

case class YandexTranslation(json: YandexTranslationJsonRepr, text: String) extends Translation {
  override val translatedText: String = json.text.mkString("; ")
  override val originalText: String   = text
}

case class YandexTranslationJsonRepr(code: Int, lang: String, text: List[String])

object YandexTranslation {

  private val codec: JsonValueCodec[YandexTranslationJsonRepr] =
    JsonCodecMaker.make(
        CodecMakerConfig
          .withFieldNameMapper(JsonCodecMaker.enforceCamelCase)
    )

  def fromJson(bytes: Array[Byte], text: String): Try[YandexTranslation] =
    Try(readFromArray(bytes)(codec))
      .map(YandexTranslation(_, text))
}
