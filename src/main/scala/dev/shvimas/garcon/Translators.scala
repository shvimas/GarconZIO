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

  trait Service extends Translators {
    protected def abbyyApiKey: String
    protected def yandexApiKey: String

    override lazy val abbyyTranslator: Translator  = AbbyyTranslator(abbyyApiKey)
    override lazy val yandexTranslator: Translator = YandexTranslator(yandexApiKey)

    override lazy val supportedTranslators: Map[String, Translator] = Map(
        CommonTranslationFields.abbyy  -> abbyyTranslator,
        CommonTranslationFields.yandex -> yandexTranslator,
    )

    override lazy val defaultTranslator: Translator = supportedTranslators(Defaults.translatorName)
  }

  trait Live extends Service {

    override protected def abbyyApiKey: String = Live.abbyyApiKey

    override protected def yandexApiKey: String = Live.yandexApiKey
  }

  private object Live {
    val abbyyApiKey: String  = config.getString("abbyy.apiKey")
    val yandexApiKey: String = config.getString("yandex.apiKey")
  }

}
