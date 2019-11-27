package dev.shvimas.garcon.model

import dev.shvimas.telegram.model.{Chat, Message}
import dev.shvimas.translate.LanguageDirection
import org.scalatest.FunSuite

class RequestParserTest extends FunSuite {

  def doTest(message: Message, expected: Request): Unit = {
    val messageToCommand = RequestParser.parseMessage(message)
    assert(messageToCommand == expected)
  }

  test("translate") {
    val text = "some text"
    val message = makeMessage(text, None)
    doTest(message, TranslationRequest(text, 1337, 1))
  }

  test("delete by reply") {
    val text = "/delete"
    val reply = makeMessage("to be deleted", None)
    val message = makeMessage(text, Some(reply))
    doTest(message, DeleteByReply(reply, 1337))
  }

  test("delete by lang dir") {
    val text = "/delete test en-ru"
    val message = makeMessage(text, None)
    doTest(message, DeleteByText("test", LanguageDirection.EN_RU, 1337))
  }

  test("bad delete by lang dir") {
    val text = "/delete test trash"
    val message = makeMessage(text, None)
    doTest(message, MalformedCommand("bad language direction: trash"))
  }

  val defaultChat =
    Chat(
      id = 1337,
      `type` = "test",
      title = None,
      username = None,
      firstName = None,
      lastName = None,
    )

  def makeMessage(text: String, replyToMessage: Option[Message]): Message =
    Message(
      messageId = 1,
      from = None,
      date = 20190809,
      chat = defaultChat,
      editDate = None,
      text = if (text.nonEmpty) Some(text) else None,
      entities = Nil,
      captionEntities = Nil,
      replyToMessage = replyToMessage,
      replyMarkup = None,
    )
}
