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
      case x: PersistenceError => {
        // This only occurs during the initial read, so populate with default
        if(storedValue.isEmpty) { storedValue = Some(defaultValue) } 
        storedValue.get
        // TODO: Log
      }
      case _ => { 
        if(storedValue.isEmpty) { storedValue = Some(defaultValue) } 
        storedValue.get
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
          case _ => None // TODO: Log
        }
        finally { persistenceLock.unlock() }
      }
    }
    
    workQueue.execute(task)
  }

}
