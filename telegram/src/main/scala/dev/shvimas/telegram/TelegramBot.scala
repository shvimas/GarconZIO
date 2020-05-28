package dev.shvimas.telegram

import java.util.concurrent.TimeUnit

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.softwaremill.sttp._
import com.typesafe.scalalogging.StrictLogging
import dev.shvimas.telegram.model._
import dev.shvimas.telegram.model.JsonImplicits._

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

case class ApiRequestError(message: String) extends Exception(message)

class TelegramBot(settings: TelegramBotSettings) extends Bot with StrictLogging {

  implicit private val defaultBackend: SttpBackend[Id, Nothing] =
    HttpURLConnectionBackend(
        SttpBackendOptions(
            FiniteDuration(20, TimeUnit.SECONDS),
            settings.proxy.map(_.toSttpBackendProxy)
        )
    )

  private val request: RequestT[Empty, Array[Byte], Nothing] =
    sttp.response(asByteArray)

  def callApi[R: JsonValueCodec](method: String, params: Map[String, String]): Try[R] =
    for {
      uri        <- Try(uri"https://api.telegram.org/bot${settings.token}/$method?$params")
      bodyEither <- Try(request.get(uri).send().body)
      body       <- bodyEither.left.map(ApiRequestError).toTry
      result     <- Try(readFromArray[R](body))
    } yield result

  def getMe: Try[GetMeResult] =
    callApi[GetMeResult]("getMe", Map.empty)

  override def getUpdates(offset: Bot.Offset): Try[GetUpdatesResult] = {
    val params: Map[String, String] = Map("offset" -> offset.value.toString)
    callApi[GetUpdatesResult]("getUpdates", params)
  }

  override def sendMessage(chatId: Chat.Id,
                           text: Option[String],
                           disableNotification: Boolean = true,
                           replyMarkup: Option[InlineKeyboardMarkup] = None,
  ): Try[SendMessageResult] = {
    var params: Map[String, String] = Map(
        "chat_id"              -> chatId.value.toString,
        "text"                 -> text.getOrElse(""),
        "disable_notification" -> disableNotification.toString
    )
    replyMarkup.foreach(markup => params += "reply_markup" -> markup.toJson)
    logger.debug(s"Sending message with \n${params.mkString("\n")}")
    val triedSendMessageResult = callApi[SendMessageResult]("sendMessage", params)
    logger.debug("Done.")
    triedSendMessageResult
  }

  def deleteMessage(chatId: Chat.Id, messageId: Message.Id): Try[BooleanResult] = {
    val params: Map[String, String] = Map("chat_id" -> chatId.value.toString, "message_id" -> messageId.value.toString)
    callApi[BooleanResult]("deleteMessage", params)
  }

  def answerCallbackQuery(queryId: CallbackQuery.Id, text: String): Try[BooleanResult] = {
    val params: Map[String, String] = Map("query_id" -> queryId.value, "text" -> text)
    callApi[BooleanResult]("answerCallbackQuery", params)
  }

}
