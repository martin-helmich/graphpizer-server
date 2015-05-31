package persistence

import java.sql.Connection

import scala.collection.mutable

class LazyCollection[T](factory: => Seq[T])(implicit c: Connection) extends Seq[T] {

  var initialized = false
  var inner: Seq[T] = null
  var added: mutable.Buffer[T] = mutable.Buffer()

  protected def initialize(): Unit = {
    if (!initialized) {
      inner = factory
    }
  }

  override def length: Int = {
    initialize()
    inner.length
  }

  override def apply(idx: Int): T = {
    initialize()
    inner.apply(idx)
  }

  override def iterator: Iterator[T] = {
    initialize()
    inner.iterator
  }

  def += (e: T) = {
    added += e
  }

}
