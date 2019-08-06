package dev.shvimas.garcon

import com.typesafe.config.{Config, ConfigFactory}

object TestConfig {
  lazy val config: Config = ConfigFactory.parseResourcesAnySyntax("test_config.conf")
}
