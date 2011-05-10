package com.banksimple.clipping

import java.io._
//import com.banksimple.util.TempFolder
import org.specs.Specification

object ToDisk extends Specification {
  type STV = SynchronizedDiskVar[String]
  type ATV = ConcurrentDiskVar[String]

  "A disk-persisting var" should {
    "honor the default value at init." in {
      val folderName = "/tmp"
      val fname = "L1"
      try {
        val default = "default"
        val v = new STV(default, fname, folderName)
        v() must be_==( default )
      } finally { 
        deleteIfThere(folderName, fname)
      }
    }

    "honor writes." in {
      val folderName = "/tmp"
      val fname = "L2"
      try {
        val t0 = System.currentTimeMillis().toString()
        val default = "default"
        val var1 = new STV(default, fname, folderName)
        var1 << t0
        var1() must be_==( t0 )
        val var2 = new STV(default, fname, folderName) // This creates a new cell to read.
                                                       // Don't do this normally, it's unsafe.
        var2() must be_==(var1())
      } finally {
        deleteIfThere(folderName, fname)
      }
    } 
  }
  
  def deleteIfThere(loc: String, fname: String) {
    val f = new File(loc, fname)
    if(f.exists()) f.delete
  }
    
}
    
