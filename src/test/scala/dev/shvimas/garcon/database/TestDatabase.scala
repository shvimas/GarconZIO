package dev.shvimas.garcon.database

import dev.shvimas.garcon.database.mongo.Mongo
import dev.shvimas.garcon.TestConfig

object TestDatabase {
  trait Instance extends Mongo.Instance {
    override val settings: Mongo.Settings = new Mongo.Settings(TestConfig.config)
  }
}
