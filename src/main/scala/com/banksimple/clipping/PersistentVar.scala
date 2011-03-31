package com.banksimple.clipping


class PersistenceError(underlying: Throwable) extends Exception
trait PersistingStrategy[A] {
  def persist(v: A): Unit
  def read(): Option[A]
}

trait StateManagementStrategy[A] {
  self: PersistentVar[A] with PersistingStrategy[A] =>
  protected def putIf(test: A => Boolean, produce: () => A): A
  protected def get(): A
}


abstract class PersistentVar[A] {
  self: PersistentVar[A] with StateManagementStrategy[A] with PersistingStrategy[A] =>

  protected var storedValue: Option[A] = None

  def default: A

  def write(v: A): A = putIf({(a) => true}, {() => v})
  def writeIf(test: A => Boolean)(v: => A): A = putIf(test, () => { v }) 
  def read(): A = get()
  def apply(): A = get()
}

