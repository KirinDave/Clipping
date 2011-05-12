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

package com.banksimple.clipping.tests

import com.banksimple.clipping._
import com.banksimple.clipping.ManagementStrategies._
import org.specs.Specification

trait NonPersistingStrategy extends PersistingStrategy[Long] {
  @volatile private var fakeStore: Long = _
  @volatile private var initialized = false

  override def persist(i: Long) { initialized = true ; fakeStore = i }

  override def reify() =
    fakeStore match {
      case uninitialized if(!initialized) => { initialized = true ; None }
      case wasInitialized => Some(fakeStore)
    }
}

class RawTestVar extends PersistentVar[Long]
                         with SyncronousManagementStrategy[Long]
                         with NonPersistingStrategy {
  override def defaultValue = 101
}

class MonadicSpec extends Specification {
  "Persistent Vars" should {
    "allow use of for comphrehension" in {
      val aRawTestVar = new RawTestVar
      val capturedTime = System.currentTimeMillis()
      aRawTestVar << capturedTime

      for( i <- aRawTestVar ) { i must be_==(capturedTime) }
    }

    "allow use of map" in {
      val aRawTestVar = new RawTestVar
      val capturedTime = System.currentTimeMillis()

      aRawTestVar << capturedTime

      val result = aRawTestVar.map(_.toString)
      result must be_==(capturedTime.toString)
    }
  }
}
