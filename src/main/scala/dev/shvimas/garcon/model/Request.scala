package dev.shvimas.garcon.model

import dev.shvimas.garcon.model.proto.callback_data._
import dev.shvimas.telegram.model.{CallbackQuery, Message, Update}
import dev.shvimas.translate.LanguageDirection
import zio.ZIO

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

case class TestStartCommand(languageDirection: Option[LanguageDirection], chatId: Int)
    extends TestCommand

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

sealed trait EditCommand extends Command

object EditCommand {
  val pattern: Regex = "edit(.*)".r
}

case class EditByReply(reply: Message, edit: String, chatId: Int) extends EditCommand

object EditByReply {
  val pattern: Regex = "\\s*(.*)\\s*".r
}

case class MalformedCommand(desc: String) extends Command

case class UnrecognisedCommand(desc: String) extends Command

case object EmptyUpdate extends Request

case object EmptyCallbackData extends Request

case object EmptyMessage extends Request

case object BothMessageAndCallback extends Request

object RequestParser {

  def parseUpdate(update: Update): ZIO[Any, Throwable, Request] =
    update match {
      case Update(_, Some(message), None)       => parseMessage(message)
      case Update(_, None, Some(callbackQuery)) => parseCallbackQuery(callbackQuery)
      case Update(_, None, None)                => ZIO.succeed(EmptyUpdate)
      case Update(_, Some(_), Some(_))          => ZIO.succeed(BothMessageAndCallback)
    }

  private[model] def parseMessage(message: Message): ZIO[Any, Throwable, Request] =
    message.text match {
      case None => ZIO.succeed(EmptyMessage)
      case Some(text) =>
        text match {
          case Command.pattern(command) =>
            ZIO.effect(parseCommand(command, message))
          case toBeTranslated =>
            ZIO.effect(
                TranslationRequest(
                    text = toBeTranslated,
                    chatId = message.chat.id,
                    messageId = message.messageId,
                )
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
      case EditCommand.pattern(rest) =>
        rest match {
          case EditByReply.pattern(edited) =>
            message.replyToMessage match {
              case Some(replyTo) => EditByReply(replyTo, edited, chatId)
              case None          => MalformedCommand("this command needs to be a part of a reply")
            }
          case other => MalformedCommand(s"""can't understand "$other"""")
        }
      case TestStartCommand.pattern(couldBeLanguageDirection) =>
        TestStartCommand(LanguageDirection.parse(couldBeLanguageDirection), chatId)
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

  private[model] def parseCallbackQuery(callbackQuery: CallbackQuery): ZIO[Any, Throwable, Request] =
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
                ZIO.effect(parseLanguageDirection(langDir, TestNextCommand(_, chatId)))
              case TestShowData(langDir, text) =>
                ZIO.effect(parseLanguageDirection(langDir, TestShowCommand(text, _, chatId)))
              case CallbackData.Empty =>
                ZIO.succeed(EmptyCallbackData)
            }
          case Failure(exception) =>
            ZIO.effect(MalformedCommand(s"Failed to parse callback data: ${exception.toString}"))
        }
      case None => ZIO.succeed(EmptyCallbackData)
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
