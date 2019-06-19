package dev.shvimas.translate.abbyy

import dev.shvimas.translate.{Common, LanguageDirection, Translation}
import dev.shvimas.zio.testing.ZioFunSuite

class AbbyyTranslatorTest extends ZioFunSuite {

  private val apiKey = Common.config.getString("abbyy.testApiKey")

  val translator: AbbyyTranslator = AbbyyTranslator(apiKey)
  assert(translator.newToken.isSuccess)

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
    val text = "cat"
    val languageDirection = LanguageDirection.EN_RU

    translator
      .translate(text, languageDirection)
      .makeTestEffect((translation: Translation) => {
        println(translation)
        assert(translation.originalText == text)
        assert(translation.translatedText == "кот, кошка")
      })
  }
}
