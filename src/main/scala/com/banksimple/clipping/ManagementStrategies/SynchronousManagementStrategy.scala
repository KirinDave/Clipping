package com.banksimple.clipping.ManagementStrategies

import com.banksimple.clipping.{PersistentVar,PersistingStrategy,StateManagementStrategy, PersistenceError}
import java.util.concurrent._
import java.util.concurrent.locks.{ReentrantReadWriteLock,ReentrantLock}


trait SyncronousManagementStrategy[A] extends StateManagementStrategy[A] {
  self: PersistentVar[A] with PersistingStrategy[A] =>
  private val rwLock = new ReentrantReadWriteLock()
  
  override def putIf(test: A => Boolean, produce: () => A): A = {
    rwLock.writeLock().lock()
    try {
      val currentVal = get() // Get will get the readlock, but that's Okay
      if(test(currentVal)) {
        produce() match {
          case v if(v != null) => { 
            persist(v)
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

}