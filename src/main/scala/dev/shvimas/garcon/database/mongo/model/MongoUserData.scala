package dev.shvimas.garcon.database.mongo.model

import dev.shvimas.garcon.database.model.{UserData, UserDataFields => Fields}
import org.mongodb.scala.bson.annotations.BsonProperty

private[mongo] case class MongoUserData(
    @BsonProperty(Fields.chatId) chatId: Long,
    @BsonProperty(Fields.langDir) languageDirection: Option[MongoLanguageDirection],
    @BsonProperty(Fields.decap) decapitalization: Option[Boolean],
)

object MongoUserData {

  def apply(userData: UserData): MongoUserData =
    MongoUserData(
        chatId = userData.chatId.value,
        languageDirection = userData.languageDirection.map(MongoLanguageDirection(_)),
        decapitalization = userData.decapitalization,
    )
}
