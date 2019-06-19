package dev.shvimas.translate

import com.typesafe.config.{Config, ConfigFactory}

object Common {
  val config: Config = ConfigFactory.load("private/secrets.conf")
}
