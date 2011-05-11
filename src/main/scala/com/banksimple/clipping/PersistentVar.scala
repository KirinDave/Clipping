/*
 * Copyright 2011 Simple Finance, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.banksimple.clipping
import com.codahale.logula.Logging


case class PersistenceError(underlying: Throwable) extends Exception
trait PersistingStrategy[A] {
  def persist(v: A): Unit
  def reify(): Option[A]
}

trait StateManagementStrategy[A] {
  self: PersistentVar[A] with PersistingStrategy[A] =>
  protected def putIf(test: A => Boolean, produce: () => A): A
  protected def get(): A
}


abstract class PersistentVar[A] extends Logging {
  self: PersistentVar[A] with StateManagementStrategy[A] with PersistingStrategy[A] =>

  protected var storedValue: Option[A] = None
  def defaultValue: A

  def write(v: A): A = putIf({(a) => true}, {() => v})
  def writeIf(test: A => Boolean)(v: => A): A = putIf(test, () => { v }) 
  def read(): A = get()
  def apply(): A = get()
  def <<(v: A) = write(v)

  def map[B](f: A => B) = f(get())
  def foreach(f: A => Unit) = f(get())
}

