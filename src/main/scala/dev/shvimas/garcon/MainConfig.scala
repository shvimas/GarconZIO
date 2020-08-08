package dev.shvimas.garcon

import com.typesafe.config.{Config, ConfigFactory}
import dev.shvimas.telegram.{ProxyAuthUsernamePassword, SocksProxy}
import zio._

case class TelegramBotConfig(token: String, maybeProxy: Option[TelegramBotConfig.Proxy])

object TelegramBotConfig {
  case class Proxy(username: String, password: String, host: String, port: Int) {

    def makeSocksProxy: Task[SocksProxy] = ZIO.effect {
      val proxyAuth = ProxyAuthUsernamePassword(username = username, password = password)
      SocksProxy(host = host, port = port, auth = Some(proxyAuth))
    }
  }
}

case class MongoConfig(username: String, password: String, host: String, port: Int)

case class TranslatorsConfig(abbyyApiKey: String, yandexApiKey: String)

object MainConfig {
  private val config: Config = ConfigFactory.parseResourcesAnySyntax("private/secrets.conf")

  val telegramBotConfig: Layer[Throwable, Has[TelegramBotConfig]] = ZIO.effect {
    val botToken = config.getString("bot.token")

    val telegramProxy = if (config.hasPath("proxy")) {
      Some(
        TelegramBotConfig.Proxy(
          username = config.getString("proxy.username"),
          password = config.getString("proxy.password"),
          host = config.getString("proxy.host"),
          port = config.getInt("proxy.port"),
        )
      )
    } else None

    TelegramBotConfig(botToken, telegramProxy)
  }.toLayer

  val mongoConfig: Layer[Throwable, Has[MongoConfig]] = ZIO.effect {
    MongoConfig(
      username = config.getString("mongo.username"),
      password = config.getString("mongo.password"),
      host = config.getString("mongo.host"),
      port = config.getInt("mongo.port"),
    )
  }.toLayer

  val translatorsConfig: Layer[Throwable, Has[TranslatorsConfig]] = ZIO.effect {
    TranslatorsConfig(
      abbyyApiKey = config.getString("abbyy.apiKey"),
      yandexApiKey = config.getString("yandex.apiKey"),
    )
  }.toLayer
}
