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

import org.neo4j.graphdb.Node

class NodeRepository(backend: BackendInterface) {

  def find(q: Query): Option[Node] = {
    val pf = propertyFilter(q)

    val cypher = s"""MATCH (n:${q.label.name} {$pf}) RETURN n LIMIT 1"""
    val result = q.properties match {
      case null => backend.execute(cypher).run()
      case m: Map[String, Any] => backend.execute(cypher).runWith(Map("props" -> q.properties))
    }

    if (result.hasNext) {
      val node = result.columnAs[Node]("n").next()
      result.close()
      Some(node)
    } else {
      None
    }
  }

  def merge(q: Query): Node = {
    val pf = propertyFilter(q)

    val cypher = s"""MERGE (n:${q.label.name} {$pf}) RETURN n"""
    val result = q.properties match {
      case null => backend.execute(cypher).run()
      case m: Map[String, Any] => backend.execute(cypher).runWith(Map("props" -> q.properties))
    }

    val node = result.columnAs[Node]("n").next()
    result.close()
    return node
  }

  protected def propertyFilter(q: Query) = q.properties match {
    case null => ""
    case map: Map[String, Any] => map map { case (key, _) => s"$key: {props}.$key" } mkString ", "
  }

}
