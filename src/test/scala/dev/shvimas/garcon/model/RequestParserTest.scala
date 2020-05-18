package dev.shvimas.garcon.model

import dev.shvimas.telegram.model.{Chat, Message}
import dev.shvimas.translate.LanguageDirection
import zio.test._
import zio.test.Assertion._

object RequestParserTest extends DefaultRunnableSpec {
  override def spec =
    suite("RequestParser suite")(
        suite("parseMessage suite")(
            messageParsingTest("translate")(
                message = makeMessage("some text", None),
                expected = TranslationRequest("some text", chatId, messageId),
            ),
            messageParsingTest("delete by reply")(
                message = makeMessage("/delete", Some(makeMessage("to be deleted", None))),
                expected = DeleteByReply(makeMessage("to be deleted", None), chatId),
            ),
            messageParsingTest("delete by lang dir")(
                message = makeMessage("/delete test en-ru", None),
                expected = DeleteByText("test", LanguageDirection.EN_RU, chatId),
            ),
            messageParsingTest("bad delete by lang dir")(
                message = makeMessage("/delete test trash", None),
                expected = MalformedCommand("bad language direction: trash"),
            ),
        ),
    )

  val chatId    = Chat.Id(1337)
  val messageId = Message.Id(1)

  val defaultChat =
    Chat(
        id = chatId,
        `type` = Chat.Type("test"),
        title = None,
        username = None,
        firstName = None,
        lastName = None,
    )

  def makeMessage(text: String, replyToMessage: Option[Message]): Message =
    Message(
        messageId = messageId,
        from = None,
        date = Message.Date(20190809),
        chat = defaultChat,
        editDate = None,
        text = if (text.nonEmpty) Some(Message.Text(text)) else None,
        entities = Nil,
        captionEntities = Nil,
        replyToMessage = replyToMessage,
        replyMarkup = None,
    )

  def messageParsingTest(label: String)(message: Message, expected: Request): ZSpec[Any, Throwable] =
    testM(label) {
      val zRequest = RequestParser.parseMessage(message)
      assertM(zRequest)(equalTo(expected))
    }
}
