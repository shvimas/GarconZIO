package dev.shvimas.translate.yandex

import dev.shvimas.translate._
import zio.test._

private object Env {
  private val apiKey = Common.config.getString("yandex.testApiKey")
  val translator     = new YandexTranslator(apiKey)
}

object YandexTranslatorTest
    extends DefaultRunnableSpec(
        suite("Yandex integration suite")(
            testM("translate ru-en")(
                Common.makeTranslationTest(
                    translator = Env.translator,
                    text = "обработать",
                    languageDirection = LanguageDirection.RU_EN,
                    expectedTranslation = "processing"
                )
            ),
            testM("translate en-ru")(
                Common.makeTranslationTest(
                    translator = Env.translator,
                    text = "apron",
                    languageDirection = LanguageDirection.EN_RU,
                    expectedTranslation = "фартук"
                )
            ),
        )
    )
