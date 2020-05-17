package dev.shvimas.garcon.testing

import dev.shvimas.garcon.Database
import dev.shvimas.garcon.testing.TestingServiceProvider.Key
import dev.shvimas.telegram.model.Chat
import dev.shvimas.translate.LanguageDirection
import zio.{Task, UIO, ZIO}
import zio.stm.TMap

import scala.collection.mutable

class TestingServiceProvider private (cache: TMap[Key, TestingService]) {
  // TODO: support per-user policies
  private val defaultPolicy = TestingService.Policy(numWords = 50, numSuccessesToDrop = 5, initialWeight = 3)

  def get(chatId: Chat.Id, languageDirection: LanguageDirection): ZIO[Database, Throwable, TestingService] = {
    val key = Key(chatId, languageDirection)
    val cachedService = for {
      maybeService <- cache.get(key).commit
      service      <- ZIO.fromOption(maybeService)
    } yield service
    cachedService.orElse(refresh(chatId, languageDirection))
  }

  def refresh(chatId: Chat.Id,
              languageDirection: LanguageDirection): ZIO[Database, Throwable, Either[String, TestingService]] = {
    val key = Key(chatId, languageDirection)
    TestingService
      .make(chatId, languageDirection, defaultPolicy)
      .tap { errorOrService: Either[String, TestingService] =>
        ZIO.fromEither(errorOrService).flatMap(cache.put(key, _).commit).orElseSucceed(())
      }
  }
}

object TestingServiceProvider {
  private case class Key(chatId: Chat.Id, languageDirection: LanguageDirection)

  def make: Task[TestingServiceProvider] =
    for {
      cache    <- TMap.empty[Key, TestingService].commit
      provider <- ZIO.effect(new TestingServiceProvider(cache))
    } yield provider
}
