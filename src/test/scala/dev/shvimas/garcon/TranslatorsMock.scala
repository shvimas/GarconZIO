package dev.shvimas.garcon

import dev.shvimas.garcon.database.model.CommonTranslationFields
import dev.shvimas.translate.Translator

object TranslatorsMock {

  val defaultMock = make(abbyy = TranslatorMock.abbyyTranslator, yandex = TranslatorMock.yandexTranslator)

  def make(abbyy: Translator, yandex: Translator): Translators.Service =
    new Translators.Service {
      override val abbyyTranslator  = abbyy
      override val yandexTranslator = yandex
      override val supportedTranslators = Map(
          CommonTranslationFields.abbyy  -> abbyyTranslator,
          CommonTranslationFields.yandex -> yandexTranslator,
      )
      override val defaultTranslator = abbyyTranslator
    }

  def make(translator: Translator): Translators.Service = make(translator, translator)
}
