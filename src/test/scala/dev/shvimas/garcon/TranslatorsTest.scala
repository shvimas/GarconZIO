package dev.shvimas.garcon

import com.typesafe.config.{Config, ConfigFactory}

object TranslatorsTest {

  object Instance extends Translators.Service {
    private val config: Config        = ConfigFactory.parseResourcesAnySyntax("private/test_api_keys.conf")
    override def abbyyApiKey: String  = config.getString("abbyy.testApiKey")
    override def yandexApiKey: String = config.getString("yandex.testApiKey")
  }
}
