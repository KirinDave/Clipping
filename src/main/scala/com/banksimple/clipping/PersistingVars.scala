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
