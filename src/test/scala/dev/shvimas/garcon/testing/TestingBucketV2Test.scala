package dev.shvimas.garcon.testing

import dev.shvimas.garcon.database.model.CommonTranslation
import zio.test._
import zio._
import zio.random._

object TestingBucketV2Test extends DefaultRunnableSpec {
  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("TestingBucketV2 suite")(
        testM("") {

        }
    )

  private def generateCommonTranslation: ZIO[Random, Nothing, CommonTranslation] =
    for {
      text <- nextString(10)
    } yield CommonTranslation(text, null, None)

}
