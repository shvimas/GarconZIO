package dev.shvimas.translate

import dev.shvimas.translate.LanguageCode.LanguageCode
import scalaz.zio.Task

import scala.util.Try

// todo: abstract over Task with R[_]
// or not...
abstract class Translator {
  protected type LanguageCodeImpl

  def translate(text: String, languageDirection: LanguageDirection): Task[Translation] =
    Task.fromTry(
      Try {
        val source = toLanguageCodeImpl(languageDirection.source)
        val target = toLanguageCodeImpl(languageDirection.target)
        translateImpl(text, source, target)
      }.flatten
    )

  protected def translateImpl(text: String,
                              srcLang: LanguageCodeImpl,
                              dstLang: LanguageCodeImpl,
                             ): Try[Translation]

  protected def toLanguageCodeImpl(languageCode: LanguageCode): LanguageCodeImpl
}
