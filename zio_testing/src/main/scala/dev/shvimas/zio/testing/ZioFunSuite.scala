package dev.shvimas.zio.testing

import org.scalactic.source.Position
import org.scalatest._
import zio._

trait ZioFunSuite extends FunSuite {

  private val thisClassName: String = this.getClass.getSimpleName

  private val defaultRuntime: DefaultRuntime = new DefaultRuntime {}

  class WrongTestCall(message: String) extends Exception(message)

  override protected def test(testName: String,
                              testTags: Tag*)(testFun: => Any)(implicit pos: Position): Unit =
    throw new WrongTestCall(s"most probably you wanted to call $thisClassName#testZio")

  def testZio[E, A](testName: String)(toRun: ZIO[Any, E, A]): Unit =
    super.test(testName)(defaultRuntime.unsafeRun(toRun))

  implicit class RunnableTestEffect[E, A](effect: IO[E, A]) {
    def runTest: Any = defaultRuntime.unsafeRun(effect)
  }

  implicit class MakeTestEffect[A](effect: IO[Any, A]) {

    def makeTestEffect(test: A => Assertion): UIO[Assertion] =
      effect.bimap(
          {
            case throwable: Throwable => Assertions.fail(throwable)
            case message: String      => Assertions.fail(message)
            case any: Any             => Assertions.fail(new RuntimeException(any.toString))
          },
          test
      )
  }

}
