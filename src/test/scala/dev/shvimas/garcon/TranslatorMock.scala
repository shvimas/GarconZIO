package dev.shvimas.garcon

import dev.shvimas.translate.{LanguageDirection, Translation, Translator}
import dev.shvimas.translate.LanguageCode.LanguageCode

import scala.util.{Failure, Success, Try}

trait TranslatorMock extends Translator {
  import TranslatorMock._

  val translations: Translations

  override protected type LanguageCodeImpl = LanguageCode

  override protected def translateImpl(text: String, srcLang: LanguageCode, dstLang: LanguageCode): Try[Translation] = {
    val maybeTranslation = for {
      perLangDir  <- translations.get(LanguageDirection(srcLang, dstLang))
      translation <- perLangDir.get(text)
    } yield new MockTranslation(text, translation)

    maybeTranslation match {
      case Some(translation) => Success(translation)
      case None              => Failure(new RuntimeException(s"translation not found for $text"))
    }
  }

  override protected def toLanguageCodeImpl(languageCode: LanguageCode): LanguageCode = languageCode
}

object TranslatorMock {
  type Translations = Map[LanguageDirection, Map[String, String]]

  val abbyyTranslations = Map(
      LanguageDirection.EN_RU -> Map(
          "cat" -> "кошка",
      ),
      LanguageDirection.RU_EN -> Map(
          "рука" -> "hand",
          "сделать" -> "совер. от делать",
          "делать" -> "do",
      ),
  )

  val yandexTranslations = Map(
      LanguageDirection.EN_RU -> Map(
          "cat" -> "кот",
      ),
    LanguageDirection.RU_EN -> Map(
        "ладонь" -> "palm",
    ),
  )

  val abbyyTranslator = {
    new TranslatorMock {
      override val translations = abbyyTranslations
    }
  }

  val yandexTranslator = {
    new TranslatorMock {
      override val translations = yandexTranslations
    }
  }

  class MockTranslation(text: String, translation: String) extends Translation {
    override val originalText   = text
    override val translatedText = translation
  }
}
