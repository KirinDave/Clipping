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
import java.io.Serializable

import com.banksimple.clipping.ManagementStrategies._
import com.banksimple.clipping.PersistingStrategies._

class SynchronizedDiskVar[A <: Serializable](default: A,
                                             uName: String,
                                             storeLocation: String = "/tmp/")
extends PersistentVar[A] with OnDiskPersistingStrategy[A] 
                         with SyncronousManagementStrategy[A] {
  override val name = uName
  override val storageLoc = storeLocation
  override def defaultValue = default
}


class ConcurrentDiskVar[A <: Serializable](default: A,
                                           uName: String,
                                           storeLocation: String = "/tmp/")
extends PersistentVar[A] with OnDiskPersistingStrategy[A] 
                         with AsyncronousManagementStrategy[A] {
  override val name = uName
  override val storageLoc = storeLocation
  override def defaultValue = default
}
