package dev.shvimas.garcon

import dev.shvimas.translate.Translator
import zio.{URIO, ZIO}

object TranslatorsOps {

  val abbyyTranslator: URIO[Translators, Translator] =
    ZIO.access[Translators](_.get.abbyyTranslator)

  val yandexTranslator: URIO[Translators, Translator] =
    ZIO.access[Translators](_.get.yandexTranslator)

  val supportedTranslators: URIO[Translators, Map[String, Translator]] =
    ZIO.access[Translators](_.get.supportedTranslators)

  val defaultTranslator: URIO[Translators, Translator] =
    ZIO.access[Translators](_.get.defaultTranslator)
}
