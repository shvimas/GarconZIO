package dev.shvimas.garcon

import dev.shvimas.garcon.database.model.{CommonTranslation, CommonTranslationFields, UserData}
import dev.shvimas.garcon.model.Text
import dev.shvimas.garcon.TranslatorsInteractionTestUtils._
import dev.shvimas.garcon.database.{Database, TestDatabase}
import dev.shvimas.telegram.model.Chat
import dev.shvimas.translate.LanguageDirection
import zio.{Task, ZIO}
import zio.test._
import zio.test.Assertion._

object TranslatorsInteractionTest
    extends DefaultRunnableSpec(
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
              assertM(ct, equalTo(expected))
            },
            testM("ABBYY translation refinement: сделать -> делать") {
              val languageDirection = LanguageDirection.RU_EN
              val ct                = getCommonTranslation("сделать", languageDirection)
              val expected          = "make"
              val abbyyTranslation  = ct.map(_.translations(CommonTranslationFields.abbyy))
              assertM(abbyyTranslation, equalTo(expected))
            },
        )
    ) {}

object TranslatorsInteractionTestUtils {

  def getCommonTranslation(text: String, languageDirection: LanguageDirection): Task[CommonTranslation] =
    for {
      text <- Text.prepareText(text, chatId).provide(database)
      ct   <- TranslatorsInteraction.commonTranslation(text, languageDirection).provide(translators)
    } yield ct

  private val translators = TranslatorsTest.Instance

  private val chatId = Chat.Id(1)

  private val database: Database = new TestDatabase.Stub {
    override def getUserData(chatId: Chat.Id) =
      ZIO.some(UserData(chatId = chatId, languageDirection = None, decapitalization = None))
  }
}
