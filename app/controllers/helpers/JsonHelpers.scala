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

import play.api.data.validation.ValidationError
import play.api.libs.json._

object JsonHelpers {

  class BadJsonTypeException extends Exception

  val mapPrimitive: (JsValue) => AnyRef = {
    case JsString(str) => str
    case JsNumber(no) => if (no.isValidInt) Int.box(no.toInt) else Double.box(no.toDouble)
    case JsBoolean(bo) => Boolean.box(bo)
    case JsNull => null
    case _ => throw new BadJsonTypeException
  }

  val mapPrimitiveOrArray: (JsValue) => AnyRef = {
    case JsArray(values) =>
      if (values forall { case JsString(_) => true case _ => false }) {
        values.map { case JsString(s) => s case _ => "" }.toArray[String]
      } else {
        throw new BadJsonTypeException
      }
    case v: JsValue => mapPrimitive(v)
  }

  class JsonObjectReads extends Reads[Map[String, AnyRef]] {
    def reads(js: JsValue): JsResult[Map[String, AnyRef]] = {
      js match {
        case JsObject(fields) =>
          try {
            val m = fields.filter {
              case (key, JsString(_) | JsNumber(_) | JsArray(_)) => true
              case _ => false
            }.map {
                      case (key, v: JsValue) => (key, mapPrimitiveOrArray(v))
                    }.toMap

            JsSuccess(m)
          } catch {
            case e: BadJsonTypeException => JsError(__, ValidationError("validate.error.badtype"))
            case _: Exception => JsError(__, ValidationError("validate.error.unknown"))
          }
        case t => JsError(__, ValidationError("validate.error.badtype", t))
      }
    }
  }

}
