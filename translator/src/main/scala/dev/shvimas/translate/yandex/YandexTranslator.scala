package dev.shvimas.translate.yandex

import com.softwaremill.sttp._
import dev.shvimas.translate._
import dev.shvimas.translate.LanguageCode.LanguageCode

import scala.concurrent.duration._
import scala.util.{Failure, Try}

class YandexTranslator(private val apiKey: String) extends Translator {

  override type LanguageCodeImpl = LanguageCode

  private val readTimeout: FiniteDuration = 5.seconds
  implicit private val backend: SttpBackend[Id, Nothing] =
    HttpURLConnectionBackend(
      SttpBackendOptions(connectionTimeout = readTimeout, proxy = None)
    )

  override def translateImpl(text: String,
                             srcLang: LanguageCode,
                             dstLang: LanguageCode
                            ): Try[YandexTranslation] = {
    val params: Map[String, String] =
      Map("key" -> apiKey, "lang" -> s"$srcLang-$dstLang", "text" -> text)

    val response: Id[Response[String]] =
      sttp.get(uri"https://translate.yandex.net/api/v1.5/tr.json/translate?$params")
        .send()

    response.body match {
      case Right(s) => YandexTranslation.fromJson(s, text)
      case Left(_) => Failure(YandexRequestError(response))
    }
  }

  override def toLanguageCodeImpl(languageCode: LanguageCode): LanguageCode =
    languageCode

  private case class YandexRequestError(response: Response[String]) extends Exception(response.toString())

}

object YandexTranslator {
  def apply(apiKey: String): YandexTranslator = new YandexTranslator(apiKey)
}
