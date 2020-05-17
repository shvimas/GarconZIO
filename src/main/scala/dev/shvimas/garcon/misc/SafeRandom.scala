package dev.shvimas.garcon.misc

import zio._

// seems like zio.random is not going to become safe
// see https://github.com/zio/zio/issues/3893
object SafeRandom {
  trait Service {
    def setSeed(seed: Long): UIO[Unit]
    def nextIntBounded(bound: Int): IO[NonPositiveBound, Int]
    def getImpl: UIO[scala.util.Random] // TODO: delete
  }

  val live: Layer[Nothing, SafeRandom] = ZLayer.succeed(new Service {
    import scala.util.{Random => SRandom}

    private val random = new SRandom()

    override def setSeed(seed: Long): UIO[Unit] =
      ZIO.succeed(random.setSeed(seed))

    override def nextIntBounded(bound: Int): IO[NonPositiveBound, Int] =
      if (bound > 0)
        ZIO.succeed(random.nextInt(bound))
      else
        ZIO.fail(NonPositiveBound(bound))

    override def getImpl: UIO[SRandom] =
      ZIO.succeed(random)
  })

  def setSeed(seed: Long): ZIO[SafeRandom, Nothing, Unit] =
    ZIO.accessM(_.get.setSeed(seed))

  def nextIntBounded(bound: Int): ZIO[SafeRandom, NonPositiveBound, Int] =
    ZIO.accessM[SafeRandom](_.get.nextIntBounded(bound))

  def getImpl: ZIO[SafeRandom, Nothing, scala.util.Random] =
    ZIO.accessM[SafeRandom](_.get.getImpl)

  case class NonPositiveBound(negativeValue: Int)
}
