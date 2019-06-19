package dev.shvimas.translate

import org.scalatest.FunSuite

class LanguageGuesserTest extends FunSuite {

  test("guess English") {
    assert(LanguageGuesser.testLanguage(LanguageCode.EN, "english"))
    assert(!LanguageGuesser.testLanguage(LanguageCode.EN, "русский"))
  }

  test("guess Russian") {
    assert(LanguageGuesser.testLanguage(LanguageCode.RU, "русский"))
    assert(!LanguageGuesser.testLanguage(LanguageCode.RU, "english"))
  }

}
