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

package com.banksimple.clipping.ManagementStrategies

import com.banksimple.clipping.{PersistentVar,PersistingStrategy,StateManagementStrategy, PersistenceError}
import java.util.concurrent._
import java.util.concurrent.locks.{ReentrantReadWriteLock,ReentrantLock}


trait AsyncronousManagementStrategy[A] extends StateManagementStrategy[A] {
  self: PersistentVar[A] with PersistingStrategy[A] =>

  private val rwLock = new ReentrantReadWriteLock()
  private val persistenceLock = new ReentrantLock()
  private val workQueue = Executors.newCachedThreadPool()

  override def putIf(test: A => Boolean, produce: () => A): A = {
    rwLock.writeLock().lock()

    try {
      val currentVal = get() // Get will get the readlock, but that's Okay

      if (test(currentVal)) {
        persistenceLock.lock() // Let's make sure other persistence is done before
                               // we start writing
        produce() match {
          case v if (v != null) => {
            persistenceLock.unlock() // We should cede this now for the async task.
            dispatchAsyncPersist(v)
            storedValue = Some(v)
            v
          }
          case _ => currentVal
        }
      } else {
        currentVal
      }
    } catch {
      case e => { get() }
    } finally {
      rwLock.writeLock().unlock()
      if (persistenceLock.isHeldByCurrentThread()) persistenceLock.unlock()
    }
  }

  override def get(): A = {
    rwLock.readLock().lock()

    try {
      if (storedValue.isEmpty) { // Uninitialized case
        storedValue = Some(
          reify() match {
            case Some(v) => v.asInstanceOf[A]
            case _       => defaultValue
          })
      }

      storedValue.get // What we actually want
    } catch {
      case PersistenceError(cause) => {
        // This only occurs during the initial read, so populate with default
        log.error("Problem attempting to reify var. Using default. Error: %s".format(cause))

        if (storedValue.isEmpty) { storedValue = Some(defaultValue) }
        storedValue.get
      } case e => {
        // Almost certainly a code error we should pass to the user
        log.error("Problem attempting to get() var. Error: %s".format(e), e)
        throw e
      }
    } finally {
      rwLock.readLock().unlock()
    }
  }

  private def dispatchAsyncPersist(v: A) {
    val task = new Runnable {
      override def run = {
        persistenceLock.lock()

        try {
          persist(v)
        } catch {
          case PersistenceError(cause) => log.error("Error persisting value: %s".format(cause), cause)
          case e => log.error("Unknown error while attempting to asynchronously persist value: %s".format(e), e)
        } finally { persistenceLock.unlock() }
      }
    }

    workQueue.execute(task)
  }

}
