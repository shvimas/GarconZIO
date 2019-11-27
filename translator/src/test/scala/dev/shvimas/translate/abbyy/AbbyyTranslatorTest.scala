package dev.shvimas.translate.abbyy

import dev.shvimas.translate.{Common, LanguageDirection}
import zio.test._
import zio.test.Assertion._

private object Env {
  private val apiKey = Common.config.getString("abbyy.testApiKey")
  val translator     = new AbbyyTranslator(apiKey)
}

object AbbyyTranslatorTest
    extends DefaultRunnableSpec(
        suite("ABBYY integration suite")(
            test("get new token") {
              assert(Env.translator.newToken.isSuccess, isTrue)
            },
            testM("translate ru-en")(
                Common.makeTranslationTest(
                    translator = Env.translator,
                    text = "обработать",
                    languageDirection = LanguageDirection.RU_EN,
                    expectedTranslation = "work (up), process; treat; machine"
                )
            ),
            testM("translate en-ru")(
                Common.makeTranslationTest(
                    translator = Env.translator,
                    text = "cat",
                    languageDirection = LanguageDirection.EN_RU,
                    expectedTranslation = "кот, кошка"
                )
            ),
        )
    )
