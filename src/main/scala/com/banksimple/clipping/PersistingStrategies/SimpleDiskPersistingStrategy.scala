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

package com.banksimple.clipping.PersistingStrategies

import com.banksimple.clipping.{PersistingStrategy, PersistenceError, PersistentVar}
import java.io.{Serializable, File,
                ObjectOutputStream,ObjectInputStream,
                FileOutputStream, FileInputStream}
import java.math.BigInteger
import java.util.Random // We don't need awesome randoms


trait OnDiskPersistingStrategy[A] extends PersistingStrategy[A] {
  self: PersistentVar[A] =>

  val storageLoc = "/tmp/"
  val name: String

  override def persist(value: A): Unit = {
    val tmpFile = new File(storageLoc, makeTempName)
    val resultFile = new File(storageLoc, name)

    try {
      val out = new ObjectOutputStream(new FileOutputStream(tmpFile))

      out.writeObject(value)
      out.close
      tmpFile.renameTo(resultFile) // Oldschool atomic.
    } catch {
      case x => {
        throw new PersistenceError(x)
      }
    }
  }

  override def reify(): Option[A] = {
    val source = new File(storageLoc, name)

    if (source.exists) {
      val in = new ObjectInputStream(new FileInputStream(source))

      try {
        Some(in.readObject.asInstanceOf[A])
      } catch {
        case e => {
          log.error("An error occured while attempting to reify %s: %s".format(
            storageLoc + name,
            e),
          e)
          None
        }
      }
    } else {
      None
    }
  }

  private def makeTempName: String = {
    val randomIntStr = new BigInteger(32, new Random()).toString(32)
    "persist-%s.tmp".format(randomIntStr)
  }
}
