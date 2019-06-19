package dev.shvimas.garcon.translate

import java.util.concurrent.ConcurrentHashMap

import dev.shvimas.garcon.Translators
import dev.shvimas.garcon.database.model.CommonTranslationFields
import dev.shvimas.garcon.translate.TestTranslations.{Translations, newTranslations}
import dev.shvimas.translate._
import dev.shvimas.translate.LanguageCode.LanguageCode

import scala.collection.concurrent
import scala.collection.JavaConverters._
import scala.util.Try

trait TestTranslations extends Translators {
  private val abbyyTranslations: Translations = newTranslations
  private val yandexTranslations: Translations = newTranslations
  private var default: Translator = _

  override lazy val abbyyTranslator: Translator = new TestTranslator(abbyyTranslations)

  override lazy val yandexTranslator: Translator = new TestTranslator(yandexTranslations)

  override lazy val supportedTranslators: Map[String, Translator] = Map(
    CommonTranslationFields.abbyy -> abbyyTranslator,
    CommonTranslationFields.yandex -> yandexTranslator,
  )

  override lazy val defaultTranslator: Translator = default

  def initTestTranslations(abbyyTranslations: Map[(String, LanguageDirection), String],
                           yandexTranslations: Map[(String, LanguageDirection), String],
                           default: String,
                          ): Unit = {
    this.abbyyTranslations.clear()
    this.abbyyTranslations ++= abbyyTranslations
    this.yandexTranslations.clear()
    this.yandexTranslations ++= yandexTranslations
    this.default = default match {
      case CommonTranslationFields.abbyy => this.abbyyTranslator
      case CommonTranslationFields.yandex => this.yandexTranslator
      case other => throw new RuntimeException(s"Unknown test translator: $other")
    }
  }
}

object TestTranslations {
  type Translations = concurrent.Map[(String, LanguageDirection), String]

  def newTranslations: Translations = new ConcurrentHashMap[(String, LanguageDirection), String]().asScala
}

case class TestTranslation(originalText: String, translatedText: String) extends Translation

class TestTranslator(translations: Translations) extends Translator {
  override protected type LanguageCodeImpl = LanguageCode

  override protected def translateImpl(text: String,
                                       srcLang: LanguageCodeImpl,
                                       dstLang: LanguageCodeImpl,
                                      ): Try[Translation] = {
    Try(translations(text -> LanguageDirection(srcLang, dstLang))).map(TestTranslation(text, _))
  }

  override protected def toLanguageCodeImpl(languageCode: LanguageCode): LanguageCodeImpl = languageCode
}