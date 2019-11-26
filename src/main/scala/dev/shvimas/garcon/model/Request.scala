package dev.shvimas.garcon.model

import dev.shvimas.garcon.model.proto.callback_data._
import dev.shvimas.telegram.model.{CallbackQuery, Message, Update}
import dev.shvimas.translate.LanguageDirection

import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex

sealed trait Request

case class TranslationRequest(text: String, chatId: Int, messageId: Int) extends Request

sealed trait Command extends Request

object Command {
  val pattern: Regex = "/(.*)".r
}

object HelpCommand extends Command {
  val pattern: Regex = "help|start".r
}

sealed trait TestCommand extends Command

case class TestStartCommand(languageDirection: LanguageDirection, chatId: Int) extends TestCommand

object TestStartCommand {
  val pattern: Regex = s"test\\s*(.*)".r
}

case class TestNextCommand(languageDirection: LanguageDirection, chatId: Int) extends TestCommand

case class TestShowCommand(text: String, languageDirection: LanguageDirection, chatId: Int)
    extends TestCommand

case class ChooseCommand(languageDirection: LanguageDirection, chatId: Int) extends Command

object ChooseCommand {
  val pattern: Regex = "choose (.*)".r
}

sealed trait DeleteCommand extends Command

object DeleteCommand {
  val pattern: Regex = "delete(.*)".r
}

case class DeleteByReply(reply: Message, chatId: Int) extends DeleteCommand

object DeleteByReply {
  val pattern: Regex = "\\s*".r
}

case class DeleteByText(text: String, languageDirection: LanguageDirection, chatId: Int)
    extends DeleteCommand

object DeleteByText {
  val pattern: Regex = "\\s+(.*)\\s+(.*)\\s*".r
}

case class DecapitalizeCommand(state: DecapitalizeCommand.State.Value) extends Command

object DecapitalizeCommand {
  val pattern: Regex = "decap\\s*(.*)\\s*".r

  object State extends Enumeration {
    val ON: State.Value  = Value("on")
    val OFF: State.Value = Value("off")

    def parse(s: String): Option[State.Value] =
      Try(withName(s)).toOption
  }

}

case class MalformedCommand(desc: String) extends Command

case class UnrecognisedCommand(desc: String) extends Command

case object EmptyUpdate extends Request

case object EmptyCallbackData extends Request

case object EmptyMessage extends Request

object RequestParser {

  def parseUpdate(update: Update): Request =
    update.message match {
      case Some(message) =>
        parseMessage(message)
      case None =>
        update.callbackQuery match {
          case Some(callbackQuery) =>
            parseCallbackQuery(callbackQuery)
          case None =>
            EmptyUpdate
        }
    }

  def parseMessage(message: Message): Request =
    message.text match {
      case None => EmptyMessage
      case Some(text) =>
        text match {
          case Command.pattern(command) => parseCommand(command, message)
          case toBeTranslated =>
            TranslationRequest(
                text = toBeTranslated,
                chatId = message.chat.id,
                messageId = message.messageId,
            )
        }
    }

  private def parseCommand(command: String, message: Message): Command = {
    val chatId = message.chat.id
    command match {
      case DeleteCommand.pattern(rest) =>
        rest match {
          case DeleteByReply.pattern() =>
            message.replyToMessage match {
              case Some(replyTo) => DeleteByReply(replyTo, chatId)
              case None          => MalformedCommand("got nothing to delete")
            }
          case DeleteByText.pattern(word, couldBeLanguageDirection) =>
            parseLanguageDirection(couldBeLanguageDirection, DeleteByText(word, _, chatId))
          case other: String => MalformedCommand(s"""can't understand "$other"""")
        }
      case TestStartCommand.pattern(couldBeLanguageDirection) =>
        parseLanguageDirection(couldBeLanguageDirection, TestStartCommand(_, chatId))
      case ChooseCommand.pattern(couldBeLanguageDirection) =>
        parseLanguageDirection(couldBeLanguageDirection, ChooseCommand(_, chatId))
      case DecapitalizeCommand.pattern(couldBeState) =>
        DecapitalizeCommand.State.parse(couldBeState) match {
          case Some(state) =>
            DecapitalizeCommand(state)
          case None =>
            MalformedCommand(s"Failed to parse '$couldBeState'")
        }
      case HelpCommand.pattern() => HelpCommand
      case other                 => UnrecognisedCommand(other)
    }
  }

  def parseCallbackQuery(callbackQuery: CallbackQuery): Request =
    callbackQuery.data match {
      case Some(data) =>
        val chatId = callbackQuery.from.id
        Try {
          val bytes = CallbackDataHelper.fromString(data)
          CallbackRequest.parseFrom(bytes).data
        } match {
          case Success(value: CallbackData) =>
            value match {
              case TestNextData(langDir) =>
                parseLanguageDirection(langDir, TestNextCommand(_, chatId))
              case TestShowData(langDir, text) =>
                parseLanguageDirection(langDir, TestShowCommand(text, _, chatId))
              case CallbackData.Empty =>
                EmptyCallbackData
            }
          case Failure(exception) =>
            MalformedCommand(s"Failed to parse callback data: ${exception.toString}")
        }
      case None => EmptyCallbackData
    }

  private def parseLanguageDirection[T <: Command](couldBeLanguageDirection: String,
                                                   makeT: LanguageDirection => T,
  ): Command =
    LanguageDirection.parse(couldBeLanguageDirection) match {
      case Some(languageDirection) =>
        makeT(languageDirection)
      case None =>
        MalformedCommand(s"bad language direction: $couldBeLanguageDirection")
    }
}
