package dev.shvimas.translate.abbyy

import com.softwaremill.sttp._
import dev.shvimas.translate._
import dev.shvimas.translate.abbyy.AbbyyTranslator._
import dev.shvimas.translate.LanguageCode.LanguageCode

import scala.util.{Failure, Success, Try}

class AbbyyTranslator(private val apiKey: String) extends Translator {

  override type LanguageCodeImpl = Int

  override def translateImpl(text: String,
                             srcLang: LanguageCodeImpl,
                             dstLang: LanguageCodeImpl,
  ): Try[AbbyyTranslation] = {
    def translationRequest(token: String): Try[AbbyyTranslation] = {
      val response: Id[Response[Array[Byte]]] = request
        .get(uri"$baseUrl/api/v1/Minicard?text=$text&srcLang=$srcLang&dstLang=$dstLang")
        .header("Authorization", f"Bearer $token")
        .contentLength(0)
        .send()

      response.body match {
        case Right(bytes) => AbbyyTranslation.fromJson(bytes)
        case Left(_)      => Failure(AbbyyRequestError(response))
      }
    }

    getCachedToken.flatMap(translationRequest) match {
      case Failure(AbbyyRequestError(response)) if response.code == StatusCodes.Unauthorized =>
        setCachedToken(newToken)
        getCachedToken.flatMap(translationRequest)
      case Failure(exception) =>
        Failure(exception)
      case Success(value) =>
        Success(value)
    }
  }

  private[this] var cachedToken: Try[String] = newToken

  private def getCachedToken: Try[String] =
    AbbyyTranslator.synchronized(cachedToken)

  //noinspection ScalaUnusedSymbol
  private def setCachedToken(newValue: => Try[String]): Unit =
    AbbyyTranslator.synchronized({
      cachedToken = newValue
    })

  private[abbyy] def newToken: Try[String] = {
    val response = sttp
      .post(uri"$baseUrl/api/v1.1/authenticate")
      .header("Authorization", f"Basic $apiKey")
      .body("")
      .send()

    response.body match {
      case Right(s) => Success(s)
      case Left(_)  => Failure(AbbyyRequestError(response))
    }
  }

  override def toLanguageCodeImpl(languageCode: LanguageCode): LanguageCodeImpl =
    languageCode match {
      case LanguageCode.EN => 1033
      case LanguageCode.RU => 1049
    }
}

private case class AbbyyRequestError[Body](response: Response[Body]) extends Exception(response.toString())

object AbbyyTranslator {
  implicit private[abbyy] val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  private val request: RequestT[Empty, Array[Byte], Nothing] = sttp.response(asByteArray)

  private val baseUrl = "https://developers.lingvolive.com"

  def apply(apiKey: String): AbbyyTranslator = new AbbyyTranslator(apiKey)
}
