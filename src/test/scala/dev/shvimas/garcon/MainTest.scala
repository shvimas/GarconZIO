package dev.shvimas.garcon

import java.util.concurrent.ConcurrentHashMap

import dev.shvimas.garcon.database.TestDatabase
import dev.shvimas.garcon.database.model.{CommonTranslation, CommonTranslationFields, UserData}
import dev.shvimas.garcon.telegram.TestBot
import dev.shvimas.garcon.telegram.TestBot.makeUpdate
import dev.shvimas.garcon.translate.TestTranslations
import dev.shvimas.translate.LanguageDirection
import dev.shvimas.zio.testing.ZioFunSuite

import scala.collection.concurrent
import scala.collection.JavaConverters._

class MainTest extends ZioFunSuite {
  val testEnvironment: TestBot with TestDatabase with TestTranslations =
    new TestBot with TestDatabase with TestTranslations

  val en_ru: LanguageDirection = LanguageDirection.EN_RU
  val ru_en: LanguageDirection = LanguageDirection.RU_EN

  val abbyyTranslations: Map[(String, LanguageDirection), String] = Map(
    "one" -> en_ru -> "один",
    "two" -> en_ru -> "два",
    "three" -> en_ru -> "три",
    "four" -> en_ru -> "четыре",
    "five" -> en_ru -> "пять",
    "шесть" -> ru_en -> "six",
    "семь" -> ru_en -> "seven",
    "восемь" -> ru_en -> "eight",
    "девять" -> ru_en -> "nine",
    "десять" -> ru_en -> "ten",
  )

  val yandexTranslations: Map[(String, LanguageDirection), String] = Map(
    "one" -> en_ru -> "один",
    "two" -> en_ru -> "два",
    "three" -> en_ru -> "три",
    "four" -> en_ru -> "четыре",
    "five" -> en_ru -> "пять",
    "шесть" -> ru_en -> "six",
    "семь" -> ru_en -> "seven",
    "восемь" -> ru_en -> "eight",
    "девять" -> ru_en -> "nine",
    "десять" -> ru_en -> "ten",
  )

  testZio("respond to many updates") {
    val badUpdates = List(
      makeUpdate(5, 105, "two-two", 101),
      makeUpdate(6, 106, "трижды", 777),
    )

    val goodUpdates = List(
      makeUpdate(2, 102, "two", 1488),
      makeUpdate(4, 104, "four", 777),
      makeUpdate(8, 108, "девять", 1488),
      makeUpdate(7, 107, "семь", 666),
    )

    val allUpdates = goodUpdates ++ badUpdates

    val initialUserData = Map(
      1337 -> Defaults.userData(1337),
      1488 -> UserData(1488, None, None),
      666 -> UserData(666, Some(LanguageDirection.RU_EN), Some(CommonTranslationFields.yandex)),
    )

    def concurrentMap(elems: CommonTranslation*): concurrent.Map[String, CommonTranslation] = {
      new ConcurrentHashMap[String, CommonTranslation]().asScala ++= elems.map(ct => ct.text -> ct)
    }

    val previousTranslations = Map(
      1337 -> en_ru -> concurrentMap(
        CommonTranslation("twelve", Some("двенадцать"), Some("12")),
      ),
      777 -> ru_en -> concurrentMap(
        CommonTranslation("сто", Some("hundred"), None),
      ),
      1337 -> ru_en -> concurrentMap(
        CommonTranslation("тысяча", None, Some("thousand")),
      ),
    )

    testEnvironment.initTestBot(allUpdates)
    testEnvironment.initTestDatabase(None, initialUserData, previousTranslations)
    testEnvironment.initTestTranslations(abbyyTranslations, yandexTranslations, CommonTranslationFields.yandex)

    val expectedOffset: Long = allUpdates.map(_.updateId).max + 1
    val expectedMessageCounter = allUpdates.length
    val expectedNewTranslations: Map[(Int, LanguageDirection), Map[String, CommonTranslation]] = Map(
      1488 -> en_ru -> Map("two" -> CommonTranslation("two", Some("два"), None)),
      777 -> en_ru -> Map("four" -> CommonTranslation("four", Some("четыре"), None)),
      1488 -> ru_en -> Map("девять" -> CommonTranslation("девять", Some("nine"), None)),
      666 -> ru_en -> Map("семь" -> CommonTranslation("семь", None, Some("seven"))),
    )
    val expectedNewUserData: Map[Int, UserData] = Map(
      1488 -> UserData(1488, None, Some(CommonTranslationFields.abbyy)),
      777 -> UserData(777, Some(en_ru), Some(CommonTranslationFields.abbyy)),
      101 -> UserData(101, Some(en_ru), Some(CommonTranslationFields.abbyy)),
    )

    Main.main
      .provide(testEnvironment)
      .map { _: Unit =>
        import testEnvironment._

        println("Bot end state:")
        pprint.pprintln(messageCounter)
        pprint.pprintln(updates.asScala)
        pprint.pprintln(sentMessages.asScala)
        println()
        println("Database end state:")
        pprint.pprintln(offset)
        pprint.pprintln(userData)
        pprint.pprintln(savedTranslations)

        assertResult(expectedOffset)(offset.get().get)
        assertResult(expectedMessageCounter)(messageCounter.get())

        assert(updates.isEmpty)

        // old translations did not change
        assert(previousTranslations.forall(x => savedTranslations.contains(x._1)))

        assertResult(previousTranslations ++ expectedNewTranslations)(savedTranslations)

        assertResult(initialUserData ++ expectedNewUserData)(userData)
      }

  }

}
