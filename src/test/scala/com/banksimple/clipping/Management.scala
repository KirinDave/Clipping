package com.banksimple.clipping.ManagementTests

import com.banksimple.clipping._
import com.banksimple.clipping.ManagementStrategies._


import org.specs.Specification

trait TestingPersistenceStrategy extends PersistingStrategy[String] {
  @volatile private var isFailed = false
  @volatile private var isFree = true
  @volatile private var datStr = "bot"
  override def persist(v: String) {
    if(!isFree) { println("isFree is not true, so isFailed is set!\n\n") ; isFailed = true }
    isFree = false
    datStr = v
    isFree = true
  }

  override def reify() = Some(datStr)
  def hasFailed: Boolean = synchronized { isFailed }
  def valPersisted = datStr
}



class SynchronizedTestVar extends PersistentVar[String] 
                            with SyncronousManagementStrategy[String]
                            with TestingPersistenceStrategy {
                              override def defaultValue = "bot"
                            }

class AsyncTestVar extends PersistentVar[String] 
                   with AsyncronousManagementStrategy[String]
                   with TestingPersistenceStrategy {
                     override def defaultValue = "bot"
                   }


class ManagementSpec extends Specification {
  "Management Strategies" should {
    "not result in collision in single-threaded use cases." in {
      val sT = new SynchronizedTestVar
      val aT = new AsyncTestVar
      val assigned = "Yerp"
      sT << assigned ; aT << assigned
      sT() must be_==(assigned)
      aT() must be_==(assigned)

      sT.valPersisted  must be_==(sT())
      aT.valPersisted  must be_==(aT())

      sT.hasFailed must beFalse
      aT.hasFailed must beFalse
    }

   "not result in collision in medium-threading use cases." in {
     import java.util.concurrent.{Executor,Executors,TimeUnit}
     import java.util.Random

     val sT = new SynchronizedTestVar
     val aT = new AsyncTestVar
     val testPool = Executors.newFixedThreadPool(10)
     val rGen = new Random(System.currentTimeMillis())
     val workers:Seq[() => Unit] = Seq.fill(5) {
       () => {
         for( time <- (1 to 100) ) {
           val (x,y) = (sT(),aT())
           if(time % 5 == 0) {
             val rS = rGen.nextInt.toString
             sT << rS
             aT << rS
           }
         }
       }
     }

     workers foreach( testPool.execute(_) )
     testPool.shutdown
     testPool.awaitTermination(5, TimeUnit.SECONDS)
     testPool.isTerminated must beTrue // Otherwise, we deadlocked
     sT.hasFailed must beFalse
     aT.hasFailed must beFalse
   }
  }

  private implicit def makeRunnable(in: () => Unit): Runnable = {
    new Runnable { override def run() = { in() } }
  }
}
