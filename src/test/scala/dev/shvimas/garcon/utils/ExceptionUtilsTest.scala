package dev.shvimas.garcon.utils

import org.scalatest.FunSuite

class ExceptionUtilsTest extends FunSuite {

  test("showThrowable") {
    val show      = ExceptionUtils.showThrowable
    val cause     = new RuntimeException("error")
    val exception = new IllegalArgumentException("illegal", cause)
    val expected =
      """java.lang.IllegalArgumentException: illegal
        |	at dev.shvimas.garcon.utils.ExceptionUtilsTest.$anonfun$new$1(ExceptionUtilsTest.scala:10)
        |	at org.scalatest.OutcomeOf.outcomeOf(OutcomeOf.scala:85)
        |	at org.scalatest.OutcomeOf.outcomeOf$(OutcomeOf.scala:83)
        |	at org.scalatest.OutcomeOf$.outcomeOf(OutcomeOf.scala:104)
        |	at org.scalatest.Transformer.apply(Transformer.scala:22)
        |	at org.scalatest.Transformer.apply(Transformer.scala:20)
        |	at org.scalatest.FunSuiteLike$$anon$1.apply(FunSuiteLike.scala:186)
        |	at org.scalatest.TestSuite.withFixture(TestSuite.scala:196)
        |	at org.scalatest.TestSuite.withFixture$(TestSuite.scala:195)
        |	at org.scalatest.FunSuite.withFixture(FunSuite.scala:1560)
        |	at org.scalatest.FunSuiteLike.invokeWithFixture$1(FunSuiteLike.scala:184)
        |	at org.scalatest.FunSuiteLike.$anonfun$runTest$1(FunSuiteLike.scala:196)
        |	at org.scalatest.SuperEngine.runTestImpl(Engine.scala:289)
        |	at org.scalatest.FunSuiteLike.runTest(FunSuiteLike.scala:196)
        |	at org.scalatest.FunSuiteLike.runTest$(FunSuiteLike.scala:178)
        |	at org.scalatest.FunSuite.runTest(FunSuite.scala:1560)
        |	at org.scalatest.FunSuiteLike.$anonfun$runTests$1(FunSuiteLike.scala:229)
        |	at org.scalatest.SuperEngine.$anonfun$runTestsInBranch$1(Engine.scala:396)
        |	at scala.collection.immutable.List.foreach(List.scala:392)
        |	at org.scalatest.SuperEngine.traverseSubNodes$1(Engine.scala:384)
        |	at org.scalatest.SuperEngine.runTestsInBranch(Engine.scala:379)
        |	at org.scalatest.SuperEngine.runTestsImpl(Engine.scala:461)
        |	at org.scalatest.FunSuiteLike.runTests(FunSuiteLike.scala:229)
        |	at org.scalatest.FunSuiteLike.runTests$(FunSuiteLike.scala:228)
        |	at org.scalatest.FunSuite.runTests(FunSuite.scala:1560)
        |	at org.scalatest.Suite.run(Suite.scala:1147)
        |	at org.scalatest.Suite.run$(Suite.scala:1129)
        |	at org.scalatest.FunSuite.org$scalatest$FunSuiteLike$$super$run(FunSuite.scala:1560)
        |	at org.scalatest.FunSuiteLike.$anonfun$run$1(FunSuiteLike.scala:233)
        |	at org.scalatest.SuperEngine.runImpl(Engine.scala:521)
        |	at org.scalatest.FunSuiteLike.run(FunSuiteLike.scala:233)
        |	at org.scalatest.FunSuiteLike.run$(FunSuiteLike.scala:232)
        |	at org.scalatest.FunSuite.run(FunSuite.scala:1560)
        |	at org.scalatest.tools.SuiteRunner.run(SuiteRunner.scala:45)
        |	at org.scalatest.tools.Runner$.$anonfun$doRunRunRunDaDoRunRun$13(Runner.scala:1346)
        |	at org.scalatest.tools.Runner$.$anonfun$doRunRunRunDaDoRunRun$13$adapted(Runner.scala:1340)
        |	at scala.collection.immutable.List.foreach(List.scala:392)
        |	at org.scalatest.tools.Runner$.doRunRunRunDaDoRunRun(Runner.scala:1340)
        |	at org.scalatest.tools.Runner$.$anonfun$runOptionallyWithPassFailReporter$24(Runner.scala:1031)
        |	at org.scalatest.tools.Runner$.$anonfun$runOptionallyWithPassFailReporter$24$adapted(Runner.scala:1010)
        |	at org.scalatest.tools.Runner$.withClassLoaderAndDispatchReporter(Runner.scala:1506)
        |	at org.scalatest.tools.Runner$.runOptionallyWithPassFailReporter(Runner.scala:1010)
        |	at org.scalatest.tools.Runner$.run(Runner.scala:850)
        |	at org.scalatest.tools.Runner.run(Runner.scala)
        |	at org.jetbrains.plugins.scala.testingSupport.scalaTest.ScalaTestRunner.runScalaTest2(ScalaTestRunner.java:131)
        |	at org.jetbrains.plugins.scala.testingSupport.scalaTest.ScalaTestRunner.main(ScalaTestRunner.java:28)
        |Caused by: java.lang.RuntimeException: error
        |	at dev.shvimas.garcon.utils.ExceptionUtilsTest.$anonfun$new$1(ExceptionUtilsTest.scala:9)
        |	... 45 more
        |""".stripMargin
    assertResult(expected)(show.show(exception))
  }

}
