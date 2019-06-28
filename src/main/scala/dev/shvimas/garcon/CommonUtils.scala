package dev.shvimas.garcon

object CommonUtils {
  def unify(unitAndValue: (Unit, Unit)): Unit = ()

  def unify[T](unit1: Unit, t: T): T = t

  def unify(units: Seq[Unit]): Unit = ()

  def unify(units: Either[Unit, Unit]): Unit = ()
}
