package dev.shvimas.garcon.database.mongo.model

import dev.shvimas.garcon.database.model.{GlobalsFields => Fields}
import org.mongodb.scala.bson.annotations.BsonProperty

private[mongo] case class MongoGlobals(@BsonProperty(Fields.offset) offset: Option[Long])
