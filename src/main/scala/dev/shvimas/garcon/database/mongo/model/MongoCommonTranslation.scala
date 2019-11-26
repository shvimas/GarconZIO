package dev.shvimas.garcon.database.mongo.model

import dev.shvimas.garcon.database.model.{CommonTranslation, CommonTranslationFields => Fields}
import org.mongodb.scala.bson.annotations.BsonProperty

private[mongo] case class MongoCommonTranslation(@BsonProperty(Fields.text)
                                                 text: String,
                                                 @BsonProperty(Fields.messageId)
                                                 messagedId: Option[Int],
                                                 @BsonProperty(Fields.translations)
                                                 translations: Map[String, String],
                                                 @BsonProperty(Fields.languageDirection)
                                                 languageDirection: MongoLanguageDirection) {

  def toCommonTranslation: CommonTranslation =
    CommonTranslation(text, translations)
}

object MongoCommonTranslation {

  def apply(commonTranslation: CommonTranslation,
            languageDirection: MongoLanguageDirection,
            messageId: Int,
  ): MongoCommonTranslation =
    MongoCommonTranslation(
        text = commonTranslation.text,
        messagedId = Some(messageId),
        translations = commonTranslation.translations,
        languageDirection = languageDirection,
    )
}
