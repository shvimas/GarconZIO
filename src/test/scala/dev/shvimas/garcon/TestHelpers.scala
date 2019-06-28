package dev.shvimas.garcon

import dev.shvimas.garcon.database.model.{CommonTranslation, CommonTranslationFields}

object TestHelpers {
  def makeCommonTranslation(text: String,
                            abbyy: Option[String],
                            yandex: Option[String],
                           ): CommonTranslation = {
    val translations = Map(
      CommonTranslationFields.abbyy -> abbyy,
      CommonTranslationFields.yandex -> yandex,
    ).flatMap { case (name, maybeTranslation) =>
      maybeTranslation.map(name -> _)
    }
    CommonTranslation(text, translations)
  }
}
