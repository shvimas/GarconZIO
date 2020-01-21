package dev.shvimas.garcon

import com.typesafe.scalalogging.LazyLogging
import dev.shvimas.garcon.database.model.CommonTranslation
import dev.shvimas.translate.LanguageDirection
import zio.ZIO

object TranslatorsInteraction extends LazyLogging {

  def commonTranslation(text: String,
                        languageDirection: LanguageDirection,
  ): ZIO[Translators, Nothing, CommonTranslation] =
    ZIO
      .access[Translators](_.supportedTranslators)
      .flatMap(
          translators =>
            ZIO.collectAllPar(
                translators.map {
                  case (name, translator) =>
                    translator
                      .translate(text, languageDirection)
                      .bimap(
                          logger.error(s"While translating $text ($languageDirection)", _),
                          name -> _.translatedText,
                      )
                      .option
                }
          )
      )
      .map(_.flatten)
      .map(translations => CommonTranslation(text, translations.toMap, None))
}
