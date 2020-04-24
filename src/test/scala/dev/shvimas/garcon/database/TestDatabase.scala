package dev.shvimas.garcon.database
import dev.shvimas.garcon.database.model.{CommonTranslation, UserData}
import dev.shvimas.garcon.model.Text
import dev.shvimas.telegram.Bot
import dev.shvimas.telegram.model.{Chat, Message}
import dev.shvimas.translate.LanguageDirection
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import zio.Task

object TestDatabase {

  trait Stub extends Database {
    override def updateOffset(offset: Bot.Offset): Task[UpdateResult] = throw new NotImplementedError()

    override def getOffset: Task[Bot.Offset] = throw new NotImplementedError()

    override def addCommonTranslation(translation: CommonTranslation,
                                      chatId: Chat.Id,
                                      languageDirection: LanguageDirection,
                                      messageId: Message.Id,
    ): Task[UpdateResult] = throw new NotImplementedError()

    override def lookUpText(text: Text.Checked,
                            languageDirection: LanguageDirection,
                            chatId: Chat.Id,
    ): Task[Option[CommonTranslation]] = throw new NotImplementedError()

    override def getUserData(chatId: Chat.Id): Task[Option[UserData]] = throw new NotImplementedError()

    override def setUserData(userData: UserData): Task[UpdateResult] = throw new NotImplementedError()

    override def setLanguageDirection(chatId: Chat.Id, languageDirection: LanguageDirection): Task[UpdateResult] =
      throw new NotImplementedError()

    override def findLanguageDirectionForMessage(chatId: Chat.Id,
                                                 text: Text.Checked,
                                                 messageId: Message.Id,
    ): Task[Option[LanguageDirection]] = throw new NotImplementedError()

    override def deleteText(text: Text.Checked,
                            langDirection: LanguageDirection,
                            chatId: Chat.Id,
    ): Task[DeleteResult] = throw new NotImplementedError()

    override def editTranslation(text: Text.Checked,
                                 edit: String,
                                 languageDirection: LanguageDirection,
                                 chatId: Chat.Id,
    ): Task[Option[UpdateResult]] = throw new NotImplementedError()

    override def getRandomWord(chatId: Chat.Id,
                               languageDirection: LanguageDirection,
    ): Task[Option[CommonTranslation]] = throw new NotImplementedError()
  }
}
