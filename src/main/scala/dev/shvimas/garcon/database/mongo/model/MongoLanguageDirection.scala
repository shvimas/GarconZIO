package dev.shvimas.garcon.database.mongo.model

import dev.shvimas.garcon.database.model.{LanguageDirectionFields => Fields}
import dev.shvimas.translate.LanguageCode.LanguageCode
import dev.shvimas.translate.LanguageDirection
import org.mongodb.scala.bson.annotations.BsonProperty

private[mongo] case class MongoLanguageDirection(@BsonProperty(Fields.source) source: LanguageCode,
                                                 @BsonProperty(Fields.target) target: LanguageCode) {
  def toLanguageDirection: LanguageDirection = LanguageDirection(source, target)
}

object MongoLanguageDirection {

  def apply(languageDirection: LanguageDirection): MongoLanguageDirection =
    new MongoLanguageDirection(languageDirection.source, languageDirection.target)
}
