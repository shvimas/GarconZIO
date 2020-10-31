//package dev.shvimas.translate.yandex
//
//import dev.shvimas.translate._
//import zio.test._
//
//object YandexTranslatorTest extends DefaultRunnableSpec {
//  val apiKey     = Common.config.getString("yandex.testApiKey")
//  val translator = new YandexTranslator(apiKey)
//
//  override def spec =
//    suite("Yandex integration suite")(
//        testM("translate ru-en")(
//            Common.makeTranslationTest(
//                translator = translator,
//                text = "обработать",
//                languageDirection = LanguageDirection.RU_EN,
//                expectedTranslation = "processing"
//            )
//        ),
//        testM("translate en-ru")(
//            Common.makeTranslationTest(
//                translator = translator,
//                text = "apron",
//                languageDirection = LanguageDirection.EN_RU,
//                expectedTranslation = "фартук"
//            )
//        ),
//    )
//}
