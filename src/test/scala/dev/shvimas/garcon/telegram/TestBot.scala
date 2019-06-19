package dev.shvimas.garcon.telegram

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.LinkedBlockingQueue

import com.typesafe.scalalogging.StrictLogging
import dev.shvimas.telegram.model._
import dev.shvimas.telegram.model.Result._
import dev.shvimas.telegram.Bot

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

class TestBot extends Bot with StrictLogging {

  import TestBot._

  val messageCounter: AtomicInteger = new AtomicInteger(0)

  val updates: LinkedBlockingQueue[Update] = new LinkedBlockingQueue[Update]()

  val sentMessages: LinkedBlockingQueue[Message] = new LinkedBlockingQueue[Message]()

  def initTestBot(updates: List[Update]): Unit = {
    updates.foreach(this.updates.put)
  }

  private def getUpdatesResult: GetUpdatesResult = {
    val currentUpdates = new java.util.LinkedList[Update]()
    updates.drainTo(currentUpdates)
    new GetUpdatesResult(true, currentUpdates.asScala.toList)
  }

  override def getUpdates(offset: Long): Try[GetUpdatesResult] =
    Success(getUpdatesResult)

  override def sendMessage(chatId: Int,
                           text: Option[String],
                           disableNotification: Boolean,
                          ): Try[SendMessageResult] = {
    val messageId = messageCounter.getAndIncrement()
    logger.info(s"Sending message $text to $chatId")
    val message = makeMessage(messageId, text, chatId)
    sentMessages.put(message)
    Success(
      new SendMessageResult(true, message
      )
    )
  }
}

object TestBot {

  def makeChat(chatId: Int): Chat =
    Chat(
      id = chatId,
      `type` = "",
      title = None,
      username = None,
      firstName = None,
      lastName = None,
    )

  def makeMessage(id: Int, text: Option[String], chatId: Int): Message =
    Message(
      messageId = id,
      from = None,
      date = 0,
      chat = makeChat(chatId),
      editDate = None,
      text = text,
      entities = Nil,
      captionEntities = Nil,
    )

  def makeUpdate(updateId: Int, messageId: Int, text: String, chatId: Int): Update =
    Update(
      updateId = updateId,
      message = Some(makeMessage(messageId, Some(text), chatId)),
      callbackQuery = None,
    )
}
