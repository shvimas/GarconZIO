package dev.shvimas.translate

import dev.shvimas.translate.LanguageCode.LanguageCode
import zio.Task

import scala.util.Try

abstract class Translator {
  protected type LanguageCodeImpl

  def translate(text: String, languageDirection: LanguageDirection): Task[Translation] =
    Task.fromTry(
        // important to cover it all with Try as exceptions may occur during constructing inner Tries
        Try {
          val source = toLanguageCodeImpl(languageDirection.source)
          val target = toLanguageCodeImpl(languageDirection.target)
          translateImpl(text, source, target)
        }.flatten
    )

  protected def translateImpl(text: String, srcLang: LanguageCodeImpl, dstLang: LanguageCodeImpl): Try[Translation]

  protected def toLanguageCodeImpl(languageCode: LanguageCode): LanguageCodeImpl
}
