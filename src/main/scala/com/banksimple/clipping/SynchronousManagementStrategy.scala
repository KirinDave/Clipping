package com.banksimple.clipping
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
    // TODO
  }

}
