package domain.modelgeneration

import domain.model.DataType
import domain.modelgeneration.ClassResolver.ImportContext
import play.api.Logger

class TypeResolver {

  def resolveType(typename: String, context: ImportContext): DataType = {
    val primitive = (n: String) => DataType(n, primitive = true)

    val collection = """^(.+)\[\]$""".r
    val nested = """^(.+)<(.+)>$""".r

    typename match {
      case "int"|"integer" => primitive("integer")
      case "str"|"string" => primitive("string")
      case "bool"|"boolean" => primitive("boolean")
      case "float"|"double"|"decimal" => primitive("double")
      case "void"|"null"|"nil" => primitive("null")
      case "array" => DataType("array", primitive = true, collection = true)
      case "callable" => primitive("callable")
      case "object" => primitive("object")
      case "mixed" => null
      case nested(outer, inner) =>
        val innerType = resolveType (inner, context)
        println(s"$outer<${innerType.name}>")
        DataType(s"$outer<${innerType.name}>", primitive = innerType.primitive, collection = true, inner = innerType)
      case collection(inner) =>
        val innerType = resolveType (inner, context)
        println(s"array<${innerType.name}>")
        DataType(s"array<${innerType.name}>", primitive = innerType.primitive, collection = true, inner = innerType)
      case s: String =>
        DataType(context resolveImportedName (s stripPrefix "\\"), primitive = false)
      case unknown => Logger.warn("Unknown type: " + unknown); null
    }
  }

}
