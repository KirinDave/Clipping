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

import java.io._
import org.specs.Specification

object ToDisk extends Specification {
  type STV = SynchronizedDiskVar[String]
  type ATV = ConcurrentDiskVar[String]

  "A disk-persisting var" should {
    "honor the default value at init." in {
      val folderName = "/tmp"
      val fileName = "L1"

      try {
        val default = "default"
        val v = new STV(default, fileName, folderName)

        v() must be_==( default )
      } finally {
        deleteIfThere(folderName, fileName)
      }
    }

    "honor writes." in {
      val folderName = "/tmp"
      val fname = "L2"

      try {
        val capturedTime = System.currentTimeMillis().toString()
        val default = "default"
        val var1 = new STV(default, fname, folderName)

        var1 << capturedTime
        var1() must be_==( capturedTime )

        val var2 = new STV(default, fname, folderName) // This creates a new cell to read.
                                                       // Don't do this normally, it's unsafe.
        var2() must be_==(var1())
      } finally {
        deleteIfThere(folderName, fname)
      }
    }
  }

  def deleteIfThere(loc: String, fname: String) {
    val file = new File(loc, fname)

    if(file.exists()) file.delete
  }
}

