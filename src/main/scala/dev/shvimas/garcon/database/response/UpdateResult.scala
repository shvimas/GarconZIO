package dev.shvimas.garcon.database.response

case class UpdateResult(wasAcknowledged: Boolean,
                        matchedCount: Long,
                        modifiedCount: Long)
