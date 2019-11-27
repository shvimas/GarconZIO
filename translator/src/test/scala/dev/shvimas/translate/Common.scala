package dev.shvimas.translate

import com.typesafe.config.{Config, ConfigFactory}
import zio.{Task, ZIO}
import zio.test.{assertM, Assertion, TestResult}
import zio.test.Assertion.{equalTo, hasField}

object Common {
  val config: Config = ConfigFactory.load("private/secrets.conf")

  def makeTranslationTest(translator: Translator,
                          text: String,
                          languageDirection: LanguageDirection,
                          expectedTranslation: String,
  ): ZIO[Any, Throwable, TestResult] = {
    val checkTranslatedText: Assertion[Translation] =
      hasField("translatedText", _.translatedText, equalTo(expectedTranslation))
    val checkText: Assertion[Translation] =
      hasField("text", _.originalText, equalTo(text))

    val translation: Task[Translation] =
      translator.translate(text, languageDirection)

    assertM(translation, checkTranslatedText && checkText)
  }
}
