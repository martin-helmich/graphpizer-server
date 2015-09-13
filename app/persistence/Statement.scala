package persistence

/**
 * GraPHPizer source code analytics engine
 * Copyright (C) 2015  Martin Helmich <kontakt@martin-helmich.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.neo4j.graphdb.{Result, GraphDatabaseService}
import play.api.Logger

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class Statement(graph: GraphDatabaseService, cypher: String) {

  val logger = Logger

  protected var params: Map[String, AnyRef] = null

  def run(): Result = {
    //logger.info(s"Executing cypher statement $cypher")
    val result = if (params != null) graph.execute(cypher, params) else graph.execute(cypher)
    //logger.info(s"Done")
    result
  }

  def runWith(params: Map[String, AnyRef]): Result = graph.execute(cypher, mapAsJavaMap[String, AnyRef](params))

//  def convertColumn[T <: Node](no: Integer, columns: java.util.List[String], row: java.util.Map[String, AnyRef]) = {
//    new Node(row.get(columns(no)).asInstanceOf[org.neo4j.graphdb.Node])
//  }

  def params(p: Map[String, AnyRef]): Statement = {
    params = p
    this
  }

  def convertColumn[T](no: Integer, columns: java.util.List[String], row: java.util.Map[String, AnyRef]): T = {
    row.get(columns(no)).asInstanceOf[T]
  }

  def map[X, T](m: (X) => T): Seq[T] = {
    val result = run()
    val columns = result.columns()
    try {
      result.toArray map { convertColumn[X](0, columns, _) } map m
    } finally {
      result.close()
    }
  }

  def filter[X](m: (X) => Boolean): Seq[X] = {
    val result = run()
    val columns = result.columns()
    try {
      result.toArray map { convertColumn[X](0, columns, _) } filter m
    } finally {
      result.close()
    }
  }

  def foreach[X](m: (X) => _): Unit = {
    val result = run()
    val columns = result.columns()
    try {
      while (result.hasNext) {
        val row = result.next()
        val p = convertColumn[X](0, columns, row)
        m(p)
      }
    } finally {
      result.close()
    }
  }

  def foreach[X, Y](m: (X, Y) => _): Unit = {
    val result = run()
    val columns = result.columns()
    try {
      while (result.hasNext) {
        val row = result.next()
        val x = convertColumn[X](0, columns, row)
        val y = convertColumn[Y](1, columns, row)
        m(x, y)
      }
    } finally {
      result.close()
    }
  }

  def map[X, Y, T](m: (X, Y) => T): Seq[T] = {
    val result = run()
    val columns = result.columns()
    try {
      result.toArray map { r => (convertColumn[X](0, columns, r), convertColumn[Y](1, columns, r)) } map { t => m(t._1, t._2) }
    } finally {
      result.close()
    }
  }

  def foreach[X, Y, Z](m: (X, Y, Z) => _): Unit = {
    val result = run()
    val columns = result.columns()
    try {
      while (result.hasNext) {
        val row = result.next()
        val x = convertColumn[X](0, columns, row)
        val y = convertColumn[Y](1, columns, row)
        val z = convertColumn[Z](2, columns, row)
        m(x, y, z)
      }
    } finally {
      result.close()
    }
  }

  def map[X, Y, Z, T](m: (X, Y, Z) => T): Seq[T] = {
    val result = run()
    val columns = result.columns()
    try {
      result.toArray map { r =>
        (
          convertColumn[X](0, columns, r),
          convertColumn[Y](1, columns, r),
          convertColumn[Z](2, columns, r)
        ) } map { t => m(t._1, t._2, t._3) }
    } finally {
      result.close()
    }
  }

  def map[X, Y, Z, Z1, T](m: (X, Y, Z, Z1) => T): Seq[T] = {
    val result = run()
    val columns = result.columns()
    try {
      result.toArray map { r =>
        (
          convertColumn[X](0, columns, r),
          convertColumn[Y](1, columns, r),
          convertColumn[Z](2, columns, r),
          convertColumn[Z1](3, columns, r)
        ) } map { t => m(t._1, t._2, t._3, t._4) }
    } finally {
      result.close()
    }
  }


  def foreach[X, Y, Z, X1](m: (X, Y, Z, X1) => _): Unit = {
    val result = run()
    val columns = result.columns()
    try {
      result foreach { (row) =>
        m(
          convertColumn[X](0, columns, row),
          convertColumn[Y](1, columns, row),
          convertColumn[Z](2, columns, row),
          convertColumn[X1](3, columns, row)
        )
      }
    } finally {
      result.close()
    }
  }
  def foreach[X, Y, Z, X1, Y1](m: (X, Y, Z, X1, Y1) => _): Unit = {
    val result = run()
    val columns = result.columns()
    try {
      result foreach { (row) =>
        m(
          convertColumn[X](0, columns, row),
          convertColumn[Y](1, columns, row),
          convertColumn[Z](2, columns, row),
          convertColumn[X1](3, columns, row),
          convertColumn[Y1](4, columns, row)
        )
      }
    } finally {
      result.close()
    }
  }

}
