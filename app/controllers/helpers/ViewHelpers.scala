package controllers.helpers

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

import domain.model.{ClassLike, DataType}
import org.neo4j.graphdb.Node
import play.api.libs.json.{Json, JsValue}
import persistence.NodeWrappers._
import play.api.mvc.{AnyContent, Request}

object ViewHelpers {

  def writeTypeRef(p: String, t: Node)(implicit request: Request[AnyContent]): JsValue = {
    Json.obj(
      "name" -> t.property[String]("name"),
      "__href" -> controllers.routes.Types.show(p, t.property[String]("slug").get).absoluteURL(),
      "__id" -> t.id
    )
  }

  def writeTypeRef(p: String, t: DataType)(implicit request: Request[AnyContent]): JsValue = {
    Json.obj(
      "name" -> t.name,
      "__href" -> controllers.routes.Types.show(p, t.slug).absoluteURL()
    )
  }

  def writeClassRef(p: String, c: Node)(implicit request: Request[AnyContent]): JsValue = {
    Json.obj(
      "fqcn" -> c.property[String]("fqcn"),
      "__href" -> controllers.routes.Classes.show(p, c ! "slug").absoluteURL(),
      "__id" -> c.id
    )
  }

  def writeClassRef(p: String, c: ClassLike)(implicit request: Request[AnyContent]): JsValue = {
    Json.obj(
      "fqcn" -> c.fqcn,
      "__href" -> controllers.routes.Classes.show(p, c.slug).absoluteURL()
    )
  }

}
