package dev.shvimas.garcon

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import dev.shvimas.garcon.database.Database
import dev.shvimas.garcon.database.mongo.Mongo
import dev.shvimas.telegram._

import scala.util.Try

object MainConfig extends LazyLogging {
  lazy val config: Config = ConfigFactory.parseResourcesAnySyntax("private/secrets.conf")

  private lazy val botToken = config.getString("bot.token")

  private lazy val proxySettings: Option[SocksProxy] =
    Try {
      val proxyAuth = ProxyAuthUsernamePassword(
          username = config.getString("proxy.username"),
          password = config.getString("proxy.password")
      )
      SocksProxy(
          host = config.getString("proxy.host"),
          port = config.getInt("proxy.port"),
          auth = Some(proxyAuth)
      )
    }.toOption

  if (proxySettings.isEmpty) {
    logger.warn("Telegram proxy settings not found!")
  }

  private lazy val botSettings = TelegramBotSettings(botToken, proxySettings)

  lazy val environment: Bot with Database with Translators =
    new TelegramBot(botSettings) with Mongo.Instance with Translators.Live

}
