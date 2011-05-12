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

package com.banksimple.clipping.ManagementTests

import com.banksimple.clipping._
import com.banksimple.clipping.ManagementStrategies._
import java.util.concurrent.{Executor,Executors,TimeUnit}
import java.util.Random
import org.specs.Specification


trait TestingPersistenceStrategy extends PersistingStrategy[String] {
  @volatile private var isFailed = false
  @volatile private var isFree = true
  @volatile private var datStr = "bot"

  override def persist(str: String) {
    if (!isFree) {
      println("isFree is not true, so isFailed is set!\n\n")
      isFailed = true
    }

    isFree = false
    datStr = str
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
      val syncVar = new SynchronizedTestVar
      val asyncVar = new AsyncTestVar
      val assigned = "Yerp"

      syncVar << assigned
      asyncVar << assigned

      syncVar() must be_==(assigned)
      asyncVar() must be_==(assigned)

      syncVar.valPersisted  must be_==(syncVar())
      asyncVar.valPersisted  must be_==(asyncVar())

      syncVar.hasFailed must beFalse
      asyncVar.hasFailed must beFalse
    }

   "not result in collision in medium-threading use cases." in {
     val syncedVar = new SynchronizedTestVar
     val asyncVar = new AsyncTestVar
     val testPool = Executors.newFixedThreadPool(10)
     val random = new Random(System.currentTimeMillis())

     val workers:Seq[() => Unit] = Seq.fill(5) {
       () => {
         for( time <- (1 to 100) ) {
           val (x,y) = (syncedVar(),asyncVar())

           if (time % 5 == 0) {
             val randomIntString = random.nextInt.toString
             syncedVar << randomIntString
             asyncVar << randomIntString
           }
         }
       }
     }

     workers foreach( testPool.execute(_) )
     testPool.shutdown
     testPool.awaitTermination(5, TimeUnit.SECONDS)

     testPool.isTerminated must beTrue // Otherwise, we deadlocked
     syncedVar.hasFailed must beFalse
     asyncVar.hasFailed must beFalse
   }
  }

  "withstand the pain of write-heavy worlds." in {
    val syncedVar = new SynchronizedTestVar
    val asyncVar = new AsyncTestVar
    val testPool = Executors.newFixedThreadPool(50)
    val random = new Random(System.currentTimeMillis())

    val workers: Seq[() => Unit] = Seq.fill(1000) {
      () => {
        for ( time <- (1 to 1000) ) {
          val (x, y) = (syncedVar(), asyncVar())
            if (time % 2 == 0) {
              val rS = random.nextInt.toString
              syncedVar << rS
              asyncVar << rS
            }
        }
      }
    }

    workers foreach( testPool.execute(_) )
    testPool.shutdown
    testPool.awaitTermination(30, TimeUnit.SECONDS)

    testPool.isTerminated must beTrue // Otherwise, we deadlocked
    syncedVar.hasFailed must beFalse
    asyncVar.hasFailed must beFalse
  }

  private implicit def makeRunnable(in: () => Unit): Runnable = {
    new Runnable { override def run() = { in() } }
  }
}

