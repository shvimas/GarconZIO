package dev.shvimas.garcon

import dev.shvimas.garcon.database.model.CommonTranslation
import dev.shvimas.translate.{LanguageDirection, Translator}
import dev.shvimas.ZIOLogging
import zio.{Task, ZIO}

object TranslatorsInteraction extends ZIOLogging {

  def commonTranslation(text: String,
                        languageDirection: LanguageDirection,
  ): ZIO[Translators, Nothing, CommonTranslation] =
    for {
      namedTranslators <- ZIO.access[Translators](_.supportedTranslators)
      allTranslations  <- ZIO.collectAllSuccessesPar(translateWith(namedTranslators, text, languageDirection))
    } yield CommonTranslation(text, allTranslations.toMap, None)

  private def translateWith(translators: Map[String, Translator],
                            text: String,
                            languageDirection: LanguageDirection,
  ): Iterable[Task[(String, String)]] =
    translators.map {
      case (name, translator) =>
        translator
          .translate(text, languageDirection)
          .tapError(zioLogger.error(s"While translating $text ($languageDirection)", _))
          .map(name -> _.translatedText)
    }
}
