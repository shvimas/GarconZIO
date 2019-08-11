package dev.shvimas.garcon.model

import dev.shvimas.telegram.model.Message
import dev.shvimas.translate.LanguageDirection

import scala.util.matching.Regex

sealed trait Request

case class TranslationRequest(text: String) extends Request

sealed trait Command extends Request

object Command {
  val pattern: Regex = "/(.*)".r
}

sealed trait DeleteCommand extends Command

object DeleteCommand {
  val pattern: Regex = "delete(.*)".r
}

case class DeleteByReply(reply: Message) extends DeleteCommand

object DeleteByReply {
  val pattern: Regex = "\\s*".r
}

case class DeleteByText(text: String, languageDirection: LanguageDirection) extends DeleteCommand

object DeleteByText {
  val pattern: Regex = "\\s+(.*)\\s+(.*)\\s*".r
}

case class BadDeleteCommand(desc: String) extends DeleteCommand

case class UnrecognisedCommand(desc: String) extends Command

case object EmptyRequest extends Request


object RequestParser {
  def parse(message: Message): Request =
    message.text match {
      case None => EmptyRequest
      case Some(text) =>
        text match {
          case Command.pattern(command) => parseCommand(command, message)
          case toBeTranslated => TranslationRequest(toBeTranslated)
        }
    }

  private def parseCommand(command: String, message: Message): Command = {
    command match {
      case DeleteCommand.pattern(rest) =>
        rest match {
          case DeleteByReply.pattern() =>
            message.replyToMessage match {
              case Some(replyTo) => DeleteByReply(replyTo)
              case None => BadDeleteCommand("got nothing to delete")
            }
          case DeleteByText.pattern(word, couldBeLanguageDirection) =>
            LanguageDirection.parse(couldBeLanguageDirection) match {
              case Some(languageDirection) => DeleteByText(word, languageDirection)
              case None => BadDeleteCommand(s"bad language direction: $couldBeLanguageDirection")
            }
          case other: String => BadDeleteCommand(s"""can't understand "$other"""")
        }
      case other => UnrecognisedCommand(other)
    }
  }
}