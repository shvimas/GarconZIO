package dev.shvimas.garcon.database.mongo.model

import dev.shvimas.garcon.database.model.{CommonTranslation, CommonTranslationFields => Fields}
import org.mongodb.scala.bson.annotations.BsonProperty

private[mongo] case class MongoCommonTranslation(@BsonProperty(Fields.text)
                                                 text: String,
                                                 @BsonProperty(Fields.translations)
                                                 translations: Map[String, String],
                                                 @BsonProperty(Fields.languageDirection)
                                                 languageDirection: MongoLanguageDirection)

object MongoCommonTranslation {
  def apply(commonTranslation: CommonTranslation,
            languageDirection: MongoLanguageDirection,
           ): MongoCommonTranslation =
    MongoCommonTranslation(
      text = commonTranslation.text,
      translations = commonTranslation.translations,
      languageDirection = languageDirection,
    )
}

