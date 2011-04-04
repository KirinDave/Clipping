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
      if(test(currentVal)) {
        persistenceLock.lock() // Let's make sure other persistence is done before
                               // we start writing
        produce() match {
          case v if(v != null) => {
            persistenceLock.unlock() // We should cede this now for the async task.
            dispatchAsyncPersist(v)
            storedValue = Some(v)
            v
          }
          case _ => currentVal
        }
      } else currentVal
    }
    catch {
      case e => {
        get()
      }
    }
    finally {
      rwLock.writeLock().unlock()
      if(persistenceLock.isHeldByCurrentThread()) persistenceLock.unlock()
    }
  }

  override def get(): A = {
    rwLock.readLock().lock() // Make sure there's no
    try {
      if(storedValue.isEmpty) { // Uninitialized case
        storedValue = Some(
          reify() match {
            case Some(v) => v.asInstanceOf[A]
            case _       => defaultValue
          })
      }
      storedValue.get // What we actually want
    }
    catch {
      case PersistenceError(cause) => {
        // This only occurs during the initial read, so populate with default
        log.warning("Problem attempting to reify var. Using default. Error: %s".format(cause))
        if(storedValue.isEmpty) { storedValue = Some(defaultValue) }
        storedValue.get
      }
      case e => {
        // Almost certainly a code error we should pass to the user
        log.error("Problem attempting to get() var. Error: %s".format(e), e)
        throw e
      }
    }
    finally {
      rwLock.readLock().unlock()
    }
  }

  private def dispatchAsyncPersist(v: A) {
    val task = new Runnable {
      override def run = {
        persistenceLock.lock()
        try { persist(v) }
        catch {
          case PersistenceError(cause) => {
            log.error("Error persisting value: %s".format(cause), cause)
          }
          case e => {
            log.error("Unknown error while attempting to asynchronously persist value: %s".format(e), e)
          }
        }
        finally { persistenceLock.unlock() }
      }
    }

    workQueue.execute(task)
  }

}
