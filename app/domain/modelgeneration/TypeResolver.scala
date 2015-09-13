package domain.modelgeneration

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

import domain.model.DataType
import domain.modelgeneration.ClassResolver.ImportContext
import play.api.Logger

class TypeResolver {

  def resolveType(typename: String, context: ImportContext): Option[DataType] = {
    val primitive = (n: String) => Some(DataType(n, primitive = true))

    val collection = """^(.+)\[\]$""".r
    val nested = """^(.+)<(.+)>$""".r

    typename match {
      case "int"|"integer" => primitive("integer")
      case "str"|"string" => primitive("string")
      case "bool"|"boolean" => primitive("boolean")
      case "float"|"double"|"decimal" => primitive("double")
      case "void"|"null"|"nil" => primitive("null")
      case "array" => Some(DataType("array", primitive = true, collection = true))
      case "callable" => primitive("callable")
      case "object" => primitive("object")
      case "mixed" => None
      case nested(outer, inner) =>
        resolveType (inner, context) match {
          case Some(d: DataType) => Some(DataType(s"$outer<${d.name}>", primitive = d.primitive, collection = true, inner = Some(d)))
          case _ => Some(DataType("array", primitive = true, collection = true))
        }
      case collection(inner) =>
        resolveType (inner, context) match {
          case Some(d: DataType) => Some(DataType(s"array<${d.name}>", primitive = d.primitive, collection = true, inner = Some(d)))
          case _ => Some(DataType("array", primitive = true, collection = true))
        }
      case s: String =>
        Some(DataType(context resolveImportedName (s stripPrefix "\\"), primitive = false))
      case unknown => Logger.warn("Unknown type: " + unknown); None
    }
  }

}
