package dev.shvimas.garcon.database.mongo.model

import dev.shvimas.garcon.database.model.{CommonTranslationFields => Fields}
import org.mongodb.scala.bson.annotations.BsonProperty

private[mongo] case class MongoCommonTranslation(@BsonProperty(Fields.text) text: String,
                                                 @BsonProperty(Fields.abbyy) abbyy: Option[String],
                                                 @BsonProperty(Fields.yandex) yandex: Option[String])

