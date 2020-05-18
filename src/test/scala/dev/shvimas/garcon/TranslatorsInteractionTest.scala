package dev.shvimas.garcon

import dev.shvimas.garcon.database.model.{CommonTranslation, CommonTranslationFields, UserData}
import dev.shvimas.garcon.database.TestDatabase
import dev.shvimas.garcon.model.Text
import dev.shvimas.telegram.model.Chat
import dev.shvimas.translate.LanguageDirection
import zio.{Has, ZIO}
import zio.test._
import zio.test.Assertion._

object TranslatorsInteractionTest extends DefaultRunnableSpec {

  def getCommonTranslation(text: String,
                           languageDirection: LanguageDirection,
  ): ZIO[Translators, Throwable, CommonTranslation] =
    for {
      text <- Text.prepareText(text, chatId).provide(database)
      ct   <- TranslatorsInteraction.commonTranslation(text, languageDirection)
    } yield ct

  private val translators = (TestTranslators.config >>> Translators.live).orDie

  private val chatId = Chat.Id(1)

  private val database: Database = Has(new TestDatabase.Stub {
    override def getUserData(chatId: Chat.Id) =
      ZIO.some(UserData(chatId = chatId, languageDirection = None, decapitalization = None))
  })

  override def spec =
    suite("translators interaction suite")(
        testM("common translation for cat (en-ru)") {
          val text              = "cat"
          val languageDirection = LanguageDirection.EN_RU
          val expected = {
            val translations = Map(
                CommonTranslationFields.abbyy  -> "кот, кошка",
                CommonTranslationFields.yandex -> "кошка",
            )
            CommonTranslation(text, translations, None)
          }
          val ct = getCommonTranslation(text, languageDirection)
          assertM(ct)(equalTo(expected))
        },
        testM("ABBYY translation refinement: сделать -> делать") {
          val languageDirection = LanguageDirection.RU_EN
          val ct                = getCommonTranslation("сделать", languageDirection)
          val expected          = "make"
          val abbyyTranslation  = ct.map(_.translations(CommonTranslationFields.abbyy))
          assertM(abbyyTranslation)(equalTo(expected))
        },
    ).provideLayer(translators)
}
