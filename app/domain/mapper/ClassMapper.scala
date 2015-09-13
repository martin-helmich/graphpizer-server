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

import javax.inject.Inject

import domain.model.ModelEdgeTypes._
import domain.model.{ModelLabelTypes, Class}
import org.neo4j.graphdb.Node
import persistence.NodeWrappers._

class ClassMapper @Inject() (propertyMapper: PropertyMapper) {
  
  def mapClassToNode(c: Class, n: Node) = {
    n("name") = c.name

    c.namespace foreach { ns =>
      n("namespace") = ns
      n("fqcn") = ns + "\\" + c.name
    }

    n("abstract") = c.isAbstract
    n("final") = c.isFinal

    val existingProperties = (n >--> HAS_PROPERTY map { r => (r.end.property[String]("name").get, r.end) }).toMap
    c.properties foreach { p =>
      val propertyNode = existingProperties get p.name match {
        case Some(existing: Node) => existing
        case _ => (n --| HAS_PROPERTY |--> ModelLabelTypes.Property).>>
      }

      propertyMapper.mapPropertyToNode(p, propertyNode)
    }
  }
  
}
