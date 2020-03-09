package dev.shvimas.garcon.database.mongo.model

import dev.shvimas.garcon.database.model.{CommonTranslation, CommonTranslationFields => Fields}
import dev.shvimas.telegram.model.Message
import org.mongodb.scala.bson.annotations.BsonProperty

private[mongo] case class MongoCommonTranslation(@BsonProperty(Fields.text)
                                                 text: String,
                                                 @BsonProperty(Fields.messageId)
                                                 messagedId: Option[Long],
                                                 @BsonProperty(Fields.translations)
                                                 translations: Map[String, String],
                                                 @BsonProperty(Fields.languageDirection)
                                                 languageDirection: MongoLanguageDirection,
                                                 @BsonProperty(Fields.edited)
                                                 edited: Option[String]) {
  def toCommonTranslation: CommonTranslation =
    CommonTranslation(text, translations, edited)
}

object MongoCommonTranslation {

  def apply(commonTranslation: CommonTranslation,
            languageDirection: MongoLanguageDirection,
            messageId: Message.Id,
  ): MongoCommonTranslation =
    MongoCommonTranslation(
      text = commonTranslation.text,
      messagedId = Some(messageId.value),
      translations = commonTranslation.translations,
      languageDirection = languageDirection,
      edited = commonTranslation.edited,
    )
}
