package domain.modelgeneration

import domain.model.DataType
import domain.modelgeneration.ClassResolver.ImportContext
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import domain.astimport.DocComment.{VarTag, ReturnTag, ParamTag}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.mock._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class TypeResolverSpec extends Specification with Mockito {

  "A type resolver" should {
    val res = new TypeResolver
    val ctx = mock[ImportContext]

    "resolve integer type from" in {
      "int" in {
        res.resolveType("int", ctx) match {
          case Some(DataType("integer", true, false, null)) => success
          case x => failure(x.toString)
        }
      }
      "integer" in {
        res.resolveType("integer", ctx) match {
          case Some(DataType("integer", true, false, null)) => success
          case x => failure(x.toString)
        }
      }
    }

    "resolve string type from" in {
      "str" in {
        res.resolveType("str", ctx) match {
          case Some(DataType("string", true, false, null)) => success
          case x => failure(x.toString)
        }
      }
      "string" in {
        res.resolveType("string", ctx) match {
          case Some(DataType("string", true, false, null)) => success
          case x => failure(x.toString)
        }
      }
    }

    "resolve boolean type from" in {
      "bool" in {
        res.resolveType("bool", ctx) match {
          case Some(DataType("boolean", true, false, null)) => success
          case x => failure(x.toString)
        }
      }
      "boolean" in {
        res.resolveType("boolean", ctx) match {
          case Some(DataType("boolean", true, false, null)) => success
          case x => failure(x.toString)
        }
      }
    }

    "resolve double type from" in {
      "float" in {
        res.resolveType("float", ctx) match {
          case Some(DataType("double", true, false, null)) => success
          case x => failure(x.toString)
        }
      }
      "double" in {
        res.resolveType("double", ctx) match {
          case Some(DataType("double", true, false, null)) => success
          case x => failure(x.toString)
        }
      }
      "decimal" in {
        res.resolveType("decimal", ctx) match {
          case Some(DataType("double", true, false, null)) => success
          case x => failure(x.toString)
        }
      }
    }

    "resolve null type from" in {
      "null" in {
        res.resolveType("null", ctx) match {
          case Some(DataType("null", true, false, null)) => success
          case x => failure(x.toString)
        }
      }
      "void" in {
        res.resolveType("void", ctx) match {
          case Some(DataType("null", true, false, null)) => success
          case x => failure(x.toString)
        }
      }
      "nil" in {
        res.resolveType("nil", ctx) match {
          case Some(DataType("null", true, false, null)) => success
          case x => failure(x.toString)
        }
      }
    }

    "resolve array type from array" in {
      res.resolveType("array", ctx) match {
        case Some(DataType("array", true, true, null)) => success
        case x => failure(x.toString)
      }
    }

    "resolve object type from object" in {
      res.resolveType("object", ctx) match {
        case Some(DataType("object", true, false, null)) => success
        case x => failure(x.toString)
      }
    }

    "resolve nested primitive type from generic notation" in {
      res.resolveType("array<int>", ctx) match {
        case Some(DataType("array<integer>", true, true, DataType("integer", true, false, null))) => success
        case x => failure(x.toString)
      }
    }

    "resolve nested primitive type from array notation" in {
      res.resolveType("int[]", ctx) match {
        case Some(DataType("array<integer>", true, true, DataType("integer", true, false, null))) => success
        case x => failure(x.toString)
      }
    }

    "resolve complex object from import scope" in {
      when(ctx.resolveImportedName("Foo")).thenReturn("Bar\\Foo")
      res.resolveType("Foo", ctx) match {
        case Some(DataType("Bar\\Foo", false, false, null)) => success
        case x => failure(x.toString)
      }
    }
    "resolve complex object collection from import scope and generic notation" in {
      when(ctx.resolveImportedName("Foo")).thenReturn("Bar\\Foo")
      res.resolveType("array<Foo>", ctx) match {
        case Some(DataType("array<Bar\\Foo>", false, true, DataType("Bar\\Foo", false, false, null))) => success
        case x => failure(x.toString)
      }
    }

    "resolve complex object collection from import scope and array notation" in {
      when(ctx.resolveImportedName("Foo")).thenReturn("Bar\\Foo")
      res.resolveType("Foo[]", ctx) match {
        case Some(DataType("array<Bar\\Foo>", false, true, DataType("Bar\\Foo", false, false, null))) => success
        case x => failure(x.toString)
      }
    }

  }

}
