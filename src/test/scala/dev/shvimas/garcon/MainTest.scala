package dev.shvimas.garcon

import java.util.concurrent.ConcurrentHashMap

import dev.shvimas.garcon.database.TestDatabase
import dev.shvimas.garcon.database.model.{CommonTranslation, CommonTranslationFields, UserData}
import dev.shvimas.garcon.telegram.TestBot
import dev.shvimas.garcon.telegram.TestBot.makeUpdate
import dev.shvimas.garcon.translate.TestTranslations
import dev.shvimas.garcon.TestHelpers._
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
    "one" -> en_ru -> "1",
    "two" -> en_ru -> "2",
    "three" -> en_ru -> "3",
    "four" -> en_ru -> "4",
    "five" -> en_ru -> "5",
    "шесть" -> ru_en -> "6",
    "семь" -> ru_en -> "7",
    "восемь" -> ru_en -> "8",
    "девять" -> ru_en -> "9",
    "десять" -> ru_en -> "10",
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
      1488 -> UserData(1488, None),
      666 -> UserData(666, Some(LanguageDirection.RU_EN)),
    )

    def concurrentMap(elems: CommonTranslation*): concurrent.Map[String, CommonTranslation] = {
      new ConcurrentHashMap[String, CommonTranslation]().asScala ++= elems.map(ct => ct.text -> ct)
    }

    val previousTranslations = Map(
      1337 -> en_ru -> concurrentMap(
        makeCommonTranslation("twelve", Some("двенадцать"), Some("12")),
      ),
      777 -> ru_en -> concurrentMap(
        makeCommonTranslation("сто", Some("hundred"), None),
      ),
      1337 -> ru_en -> concurrentMap(
        makeCommonTranslation("тысяча", None, Some("1000")),
      ),
    )

    testEnvironment.initTestBot(allUpdates)
    testEnvironment.initTestDatabase(None, initialUserData, previousTranslations)
    testEnvironment.initTestTranslations(abbyyTranslations, yandexTranslations, CommonTranslationFields.yandex)

    val expectedOffset: Long = allUpdates.map(_.updateId).max + 1
    val expectedMessageCounter = allUpdates.length
    val expectedNewTranslations: Map[(Int, LanguageDirection), Map[String, CommonTranslation]] = Map(
      1488 -> en_ru -> Map("two" -> makeCommonTranslation("two", Some("два"), Some("2"))),
      777 -> en_ru -> Map("four" -> makeCommonTranslation("four", Some("четыре"), Some("4"))),
      1488 -> ru_en -> Map("девять" -> makeCommonTranslation("девять", Some("nine"), Some("9"))),
      666 -> ru_en -> Map("семь" -> makeCommonTranslation("семь", Some("seven"), Some("7"))),
    )
    val expectedNewUserData: Map[Int, UserData] = Map(
      1488 -> UserData(1488, Some(en_ru)),
      777 -> UserData(777, Some(en_ru)),
      101 -> UserData(101, Some(en_ru)),
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
