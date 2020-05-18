package dev.shvimas.garcon

import com.typesafe.config.{Config, ConfigFactory}
import zio._

object TestTranslators {

  val config: Layer[Throwable, Has[TranslatorsConfig]] = ZLayer.fromEffect(
      ZIO.effect {
        val config: Config = ConfigFactory.parseResourcesAnySyntax("private/test_api_keys.conf")
        TranslatorsConfig(
            abbyyApiKey = config.getString("abbyy.testApiKey"),
            yandexApiKey = config.getString("yandex.testApiKey"),
        )
      }
  )
}
