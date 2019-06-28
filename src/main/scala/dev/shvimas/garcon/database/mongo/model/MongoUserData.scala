package dev.shvimas.garcon.database.mongo.model

import dev.shvimas.garcon.database.model.{UserData, UserDataFields => Fields}
import org.mongodb.scala.bson.annotations.BsonProperty

private[mongo] case class MongoUserData(@BsonProperty(Fields.chatId) chatId: Int,
                                        @BsonProperty(Fields.langDir) languageDirection: Option[MongoLanguageDirection])

object MongoUserData {
  def apply(userData: UserData): MongoUserData =
    MongoUserData(
      chatId = userData.chatId,
      languageDirection = userData.languageDirection.map(MongoLanguageDirection(_)),
    )
}
