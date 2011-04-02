package com.banksimple.clipping

import java.io._
import com.banksimple.util.TempFolder
import org.specs.Specification

object ToDisk extends Specification with TempFolder {
  type STV = SynchronizedDiskVar[String]
  type ATV = ConcurrentDiskVar[String]

  "A disk-persisting var" should {
    "honor the default value at init." in {
      withTempFolder {
        val default = "default"
        val v = new STV(default, "L1", folderName)
        v() must be_==( default )
      }
    }

    "honor writes." in {
      withTempFolder { 
        val t0 = System.currentTimeMillis().toString()
        val default = "default"
        val var1 = new STV(default, "L1", folderName)
        var1 << t0
        var1() must be_==( t0 )


        val var2 = new STV(default, "L1", folderName) // This creates a new cell to read.
                                                        // Don't do this normally, it's unsafe.
        var2() must be_==(var1())
      } 
    }
  }

}
    
