package controllers.dto

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

import java.util.UUID

import play.api.Logger

case class Node(labels: Seq[String], properties: Map[String, Any], merge: Option[Boolean]) {

  val id = properties get "__node_id" match {
    case Some(i) => i.asInstanceOf[String]
    case _ =>
      Logger.warn("Node " + labels + " does not have an id")
      UUID.randomUUID().toString
  }

}
case class Edge(from: String, to: String, label: String, properties: Map[String, Any])
case class ImportDataSet(nodes: Seq[Node], edges: Seq[Edge])