package dev.shvimas.translate.yandex

import com.softwaremill.sttp._
import dev.shvimas.translate._
import dev.shvimas.translate.LanguageCode.LanguageCode
import dev.shvimas.translate.yandex.YandexTranslator._

import scala.concurrent.duration._
import scala.util.{Failure, Try}

class YandexTranslator(private val apiKey: String) extends Translator {

  override type LanguageCodeImpl = LanguageCode

  override def translateImpl(text: String, srcLang: LanguageCode, dstLang: LanguageCode): Try[YandexTranslation] = {
    val params: Map[String, String] =
      Map("key" -> apiKey, "lang" -> s"$srcLang-$dstLang", "text" -> text)

    val response: Id[Response[Array[Byte]]] =
      request
        .get(uri"https://translate.yandex.net/api/v1.5/tr.json/translate?$params")
        .send()

    response.body match {
      case Right(s) => YandexTranslation.fromJson(s, text)
      case Left(_)  => Failure(YandexRequestError(response))
    }
  }

  override def toLanguageCodeImpl(languageCode: LanguageCode): LanguageCode =
    languageCode

  private case class YandexRequestError[Body](response: Response[Body]) extends Exception(response.toString())

}

object YandexTranslator {
  implicit private[yandex] val backend: SttpBackend[Id, Nothing] = {
    val readTimeout: FiniteDuration = 5.seconds
    HttpURLConnectionBackend(
        SttpBackendOptions(connectionTimeout = readTimeout, proxy = None)
    )
  }

  private val request: RequestT[Empty, Array[Byte], Nothing] = sttp.response(asByteArray)

  def apply(apiKey: String): YandexTranslator = new YandexTranslator(apiKey)
}
