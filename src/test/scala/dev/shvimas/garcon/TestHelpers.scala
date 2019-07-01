package dev.shvimas.garcon

import com.mongodb.client.result.UpdateResult
import dev.shvimas.garcon.database.model.{CommonTranslation, CommonTranslationFields}
import org.mongodb.scala.bson.BsonObjectId

object TestHelpers {
  def makeCommonTranslation(text: String,
                            abbyy: Option[String],
                            yandex: Option[String],
                           ): CommonTranslation = {
    val translations = Map(
      CommonTranslationFields.abbyy -> abbyy,
      CommonTranslationFields.yandex -> yandex,
    ).flatMap { case (name, maybeTranslation) =>
      maybeTranslation.map(name -> _)
    }
    CommonTranslation(text, translations)
  }

  def makeUpdateResult(matchedCount: Long, modifiedCount: Long): UpdateResult =
    UpdateResult.acknowledged(matchedCount, modifiedCount, BsonObjectId())
}
