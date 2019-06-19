package dev.shvimas.translate.yandex

import dev.shvimas.translate._
import dev.shvimas.zio.testing.ZioFunSuite

class YandexTranslatorTest extends ZioFunSuite {

  private val apiKey = Common.config.getString("yandex.testApiKey")

  val translator: Translator = YandexTranslator(apiKey)

  testZio("translate ru-en") {
    val text = "обработать"
    val languageDirection = LanguageDirection.RU_EN

    translator
      .translate(text, languageDirection)
      .makeTestEffect((translation: Translation) => {
        println(translation)
        assert(translation.originalText == text)
        assert(translation.translatedText == "work (up), process; treat; machine")
      })
  }

  testZio("translate en-ru") {
    val text = "apron"
    val languageDirection = LanguageDirection.EN_RU

    translator
      .translate(text, languageDirection)
      .makeTestEffect((translation: Translation) => {
        println(translation)
        assert(translation.originalText == text)
        assert(translation.translatedText == "фартук")
      })
  }

}
