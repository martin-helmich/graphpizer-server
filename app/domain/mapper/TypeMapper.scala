package domain.mapper

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

import domain.model.ModelEdgeTypes._
import domain.model.DataType
import org.neo4j.graphdb.Node
import persistence.NodeWrappers._

import scala.collection.mutable

class TypeMapper {

  protected val knownMappings = mutable.Map[Node, DataType]()

  def mapNodeToType(n: Node): DataType = {
    knownMappings get n match {
      case Some(d: DataType) => d
      case _ =>
        val t = buildFromNode(n)
        knownMappings += n -> t
        t
    }
  }

  protected def buildFromNode(n: Node): DataType = {
    val inner = n >--> COLLECTION_OF map { _.end }
    val innerType = inner.size match {
      case 0 => None
      case _ => Some(buildFromNode(inner.head))
    }

    DataType(
      n.property[String]("name").get,
      n.property[Boolean]("primitive").getOrElse(true),
      n.property[Boolean]("collection").getOrElse(false),
      innerType
    )
  }

}
