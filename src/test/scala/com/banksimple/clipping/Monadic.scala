package com.banksimple.clipping.tests

import com.banksimple.clipping._
import com.banksimple.clipping.ManagementStrategies._
import org.specs.Specification

trait NonPersistingStrategy extends PersistingStrategy[Long] {
  @volatile private var fakeStore: Long = _
  @volatile private var unInit = true
  override def persist(i: Long) { unInit = false ; fakeStore = i }
  override def reify() = 
    fakeStore match {
      case x if(unInit) => { unInit = false ; None }
      case y            => Some(fakeStore)
    }
}

class RawTestVar extends PersistentVar[Long]
                         with SyncronousManagementStrategy[Long]
                         with NonPersistingStrategy {
  override def defaultValue = 101
}

class MonadicSpec extends Specification {
  "Persistent Vars" should {
    "allow use of foreach" in { 
      val tT = new RawTestVar
      val tval = System.currentTimeMillis()
      tT << tval
      
      for( t <- tT ) { t must be_==(tval) }
    }

    "allow use of map" in {
      val tT = new RawTestVar
      val tval = System.currentTimeMillis()
      tT << tval

      val r = for( t <- tT ) yield { t.toString }
      r.head must be_==(tval)
    }
  }
}
