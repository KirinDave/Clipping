package com.banksimple.clipping.PersistingStrategies

import com.banksimple.clipping.{PersistingStrategy, PersistenceError}
import java.io.{Serializable, File,
                ObjectOutputStream,ObjectInputStream,
                FileOutputStream, FileInputStream}

trait OnDiskPersistingStrategy[A] extends PersistingStrategy[A] {
  val storageLoc = "/tmp/"
  val name: String

  override def persist(value: A): Unit = {
    try {
      val out = new ObjectOutputStream(new FileOutputStream(storageLoc + name))
      out.writeObject(value)
      out.close
    }
    catch {
      case x => throw new PersistenceError(x) // TODO: Log
    }
  }

  override def reify(): Option[A] = {
    val source = new File(storageLoc + name)
    if(source.exists) {
      val in = new ObjectInputStream(new FileInputStream(source))
      try { 
        Some(in.readObject.asInstanceOf[A])
      }
      catch { case _ => None  } // TODO: Log
    } else None
  }

}
