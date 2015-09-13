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

import domain.model.ClassLike.Visibility.Visibility
import domain.model.ClassLike.Visibility
import org.neo4j.graphdb.Node
import persistence.NodeWrappers._

case class Property(name: String,
                    visibility: Visibility,
                    static: Boolean = false,
                    possibleTypes: Seq[DataType] = Seq(),
                    docComment: String) {

}

object Property {

  def fromNode(n: Node): Property = new Property(
    name = n.property[String]("name").getOrElse(""),
    static = n.property[Boolean]("static").getOrElse(false),
    docComment = n.property[String]("docComment").getOrElse(""),
    possibleTypes = (n out ModelEdgeTypes.POSSIBLE_TYPE).toSeq.map { r => DataType.fromNode(r.end) },
    visibility = if (n.property[Boolean]("protected").getOrElse(false)) {
      Visibility.Protected
    } else if (n.property[Boolean]("private").getOrElse(false)) {
      Visibility.Private
    } else {
      Visibility.Public
    }
  )

}