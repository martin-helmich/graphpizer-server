package domain.model

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
import persistence.Query
import persistence.NodeWrappers._

case class DataType(name: String, primitive: Boolean, collection: Boolean = false, inner: Option[DataType] = None) {

  def classLike: Option[ClassLike] = None

  def slug = {
    name.toLowerCase.replace("\\", "-").replace("<", "--").replace(">", "")
  }

  def query = new Query(
    ModelLabelTypes.Type,
    Map(
      "name" -> name,
      "primitive" -> Boolean.box(primitive),
      "collection" -> Boolean.box(collection)
    )
  )

}

object DataType {

  def fromNode(n: Node): DataType = new DataType(
    n.property[String]("name").getOrElse(""),
    n.property[Boolean]("primitive").getOrElse(false),
    n.property[Boolean]("collection").getOrElse(false),
    (n out ModelEdgeTypes.COLLECTION_OF).headOption.map { r => fromNode(r.end) }
  ) {
    override def classLike: Option[ClassLike] =
      (n out ModelEdgeTypes.IS).headOption.map { r => ClassLike.fromNode(r.end) }
  }

}