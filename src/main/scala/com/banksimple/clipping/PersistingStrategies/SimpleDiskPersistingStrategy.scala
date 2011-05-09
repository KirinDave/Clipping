package com.banksimple.clipping.PersistingStrategies

import com.banksimple.clipping.{PersistingStrategy, PersistenceError, PersistentVar}
import java.io.{Serializable, File,
                ObjectOutputStream,ObjectInputStream,
                FileOutputStream, FileInputStream}

trait OnDiskPersistingStrategy[A] extends PersistingStrategy[A] {
  self: PersistentVar[A] =>
  val storageLoc = "/tmp/"
  val name: String

  override def persist(value: A): Unit = {
    val tmpFile = new File(storageLoc + makeTempName)
    val resultFile = new File(storageLoc + name)
    try {
      val out = new ObjectOutputStream(new FileOutputStream(tmpFile))
      out.writeObject(value)
      out.close
      tmpFile.renameTo(resultFile) // Oldschool atomic.
    }
    catch {
      case x => {
        throw new PersistenceError(x)
      }
    }
  }

  override def reify(): Option[A] = {
    val source = new File(storageLoc + name)
    if(source.exists) {
      val in = new ObjectInputStream(new FileInputStream(source))
      try {
        Some(in.readObject.asInstanceOf[A])
      }
      catch {
        case e => {
          log.error("An error occured while attempting to reify %s: %s".format(
            storageLoc + name,
            e), e)
          None }
      }
    } else None
  }

  private def makeTempName: String = {
    import java.math.BigInteger
    import java.util.Random // We don't need awesome randoms

    "persist-" + (new BigInteger(32, new Random()).toString(32)) + ".tmp"
  }

}
