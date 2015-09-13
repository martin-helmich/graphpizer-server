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

import org.neo4j.graphdb.RelationshipType
import language.implicitConversions

object ModelEdgeTypes extends Enumeration {
  type EdgeType = Value
  val DEFINED_IN, IS, HAS_PROPERTY, HAS_METHOD, HAS_PARAMETER, POSSIBLE_TYPE, COLLECTION_OF, EXTENDS, IMPLEMENTS, USES_TRAIT, USES, INSTANTIATES, DEPENDS_ON, MEMBER_OF_PACKAGE = Value

  implicit def conv(et: EdgeType): RelationshipType = new RelationshipType {
    override def name(): String = et.toString
  }
}
