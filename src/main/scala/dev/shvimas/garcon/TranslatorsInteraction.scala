package dev.shvimas.garcon

import dev.shvimas.garcon.database.model.CommonTranslation
import dev.shvimas.garcon.model.Text
import dev.shvimas.translate.{LanguageDirection, Translation, Translator}
import dev.shvimas.ZIOLogging
import dev.shvimas.translate.abbyy.AbbyyTranslator
import zio.{Task, ZIO}

import scala.util.matching.Regex

object TranslatorsInteraction extends ZIOLogging {

  def commonTranslation(text: Text.Checked,
                        languageDirection: LanguageDirection,
  ): ZIO[Translators, Nothing, CommonTranslation] =
    for {
      namedTranslators <- TranslatorsOps.supportedTranslators
      allTranslations  <- ZIO.collectAllSuccessesPar(translateWith(namedTranslators, text, languageDirection))
    } yield CommonTranslation(text.value, allTranslations.toMap, None)

  private def translateWith(translators: Map[String, Translator],
                            text: Text.Checked,
                            languageDirection: LanguageDirection,
  ): Iterable[Task[(String, String)]] =
    translators.map {
      case (name, translator) =>
        for {
          translation <- translator
            .translate(text.value, languageDirection)
            .tapError(zioLogger.error(s"While translating $text ($languageDirection)", _))
          refined <- refineTranslation(translation, languageDirection, translator)
            .tapError(zioLogger.error(s"While refining ABBYY translation ($translation)", _))
            .orElse(ZIO.succeed(translation))
        } yield name -> refined.translatedText
    }

  private def refineTranslation(translation: Translation,
                                languageDirection: LanguageDirection,
                                translator: Translator,
  ): Task[Translation] =
    translator match {
      case abbyy: AbbyyTranslator => AbbyyRefiner.refine(translation, languageDirection, abbyy)
      case _                      => ZIO.succeed(translation)
    }

  private object AbbyyRefiner {
    private val ru_enRegexToRefine: Regex = "совер. от (.*)".r

    def refine(translation: Translation,
               languageDirection: LanguageDirection,
               translator: AbbyyTranslator,
    ): Task[Translation] =
      translation.translatedText match {
        case ru_enRegexToRefine(toTranslate) if languageDirection == LanguageDirection.RU_EN =>
          translator.translate(toTranslate, languageDirection)
        case _ =>
          ZIO.succeed(translation)
      }
  }
}
