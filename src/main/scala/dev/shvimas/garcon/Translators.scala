package dev.shvimas.garcon

import dev.shvimas.garcon.database.model.CommonTranslationFields
import dev.shvimas.translate.Translator
import dev.shvimas.translate.abbyy.AbbyyTranslator
import dev.shvimas.translate.yandex.YandexTranslator
import zio.{Has, ZIO, ZLayer}

object Translators {

  trait Service {
    val abbyyTranslator: Translator
    val yandexTranslator: Translator
    val supportedTranslators: Map[String, Translator]
    val defaultTranslator: Translator
  }

  val live: ZLayer[Has[TranslatorsConfig], Throwable, Translators] = ZLayer.fromServiceM { config: TranslatorsConfig =>
    ZIO.effect {
      new Live(abbyyApiKey = config.abbyyApiKey, yandexApiKey = config.yandexApiKey)
    }
  }

  private class Live(abbyyApiKey: String, yandexApiKey: String) extends Service {
    override val abbyyTranslator: Translator  = AbbyyTranslator(abbyyApiKey)
    override val yandexTranslator: Translator = YandexTranslator(yandexApiKey)

    override val supportedTranslators: Map[String, Translator] = Map(
        CommonTranslationFields.abbyy  -> abbyyTranslator,
        CommonTranslationFields.yandex -> yandexTranslator,
    )

    override val defaultTranslator: Translator = supportedTranslators(Defaults.translatorName)
  }

}
