package persistence

import java.sql.Connection

import scala.collection.mutable
import scala.collection.mutable.BufferLike

class LazyCollection[T](factory: => Seq[T]) extends mutable.Buffer[T] {

  var initialized = false
  var inner: Seq[T] = null
  var added: mutable.Buffer[T] = mutable.Buffer()

  protected def initialize(): Unit = {
    if (!initialized) {
      inner = factory
    }
  }

  def +=(elem: T) = {
    println("Added element " + elem + " to collection")
    added += elem
    this
  }

  override def length: Int = {
    initialize()
    inner.length + added.length
  }

  override def apply(idx: Int): T = {
    initialize()
    if (idx >= inner.length) {
      inner.apply(idx)
    } else {
      added.apply(idx - inner.length)
    }
  }

  override def iterator: Iterator[T] = {
    initialize()
    inner.iterator ++ added.iterator
  }

  override def update(n: Int, newelem: T): Unit = {
    throw new Exception("Not supported")
  }

  override def clear(): Unit = {
    throw new Exception("Not supported")
  }

  override def remove(n: Int): T = {
    throw new Exception("Not supported")
  }

  override def insertAll(n: Int, elems: Traversable[T]): Unit = {
    throw new Exception("Not supported")
  }

  override def +=:(elem: T): LazyCollection.this.type = {
    println("Added element " + elem + " to collection")
    added += elem
    this
  }
}
