package dev.shvimas.garcon

import dev.shvimas.garcon.MainConfig.config
import dev.shvimas.garcon.database.model.CommonTranslationFields
import dev.shvimas.translate.Translator
import dev.shvimas.translate.abbyy.AbbyyTranslator
import dev.shvimas.translate.yandex.YandexTranslator

trait Translators {
  val abbyyTranslator: Translator
  val yandexTranslator: Translator
  val supportedTranslators: Map[String, Translator]
  val defaultTranslator: Translator
}

object Translators {

  trait Live extends Translators {

    import Live._

    override val abbyyTranslator: Translator  = AbbyyTranslator(abbyyApiKey)
    override val yandexTranslator: Translator = YandexTranslator(yandexApiKey)

    override val supportedTranslators: Map[String, Translator] = Map(
        CommonTranslationFields.abbyy  -> abbyyTranslator,
        CommonTranslationFields.yandex -> yandexTranslator,
    )

    override val defaultTranslator: Translator = supportedTranslators(Defaults.translatorName)
  }

  private object Live {
    val abbyyApiKey: String  = config.getString("abbyy.apiKey")
    val yandexApiKey: String = config.getString("yandex.apiKey")
  }

}
