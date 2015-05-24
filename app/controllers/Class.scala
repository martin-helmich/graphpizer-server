package controllers

import javax.inject.{Inject, Singleton}

import domain.model.ModelEdgeType._
import org.neo4j.graphdb.Node
import persistence.ConnectionManager
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsArray, Json}
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.concurrent.Future
import persistence.NodeWrappers._

@Singleton
class Class @Inject()(manager: ConnectionManager) extends Controller {

  def list(project: String) = Action.async {
    val future = Future {
      manager connect project transactional { (b, t) =>
        JsArray(
          b execute "MATCH (c:Class) RETURN c" map { (clazz: Node) =>
            val properties = (clazz >--> HAS_PROPERTY map { _.end } map { p =>
              Json.obj(
                "name" -> p.property[String]("name"),
                "public" -> p.property[Boolean]("public"),
                "protected" -> p.property[Boolean]("protected"),
                "private" -> p.property[Boolean]("private"),
                "static" -> p.property[Boolean]("static"),
                "docComment" -> p.property[String]("docComment")
              )
            }).toArray
            Json.obj(
              "fqcn" -> clazz.property[String]("fqcn"),
              "namespace" -> clazz.property[String]("namespace"),
              "final" -> clazz.property[Boolean]("final"),
              "abstract" -> clazz.property[Boolean]("abstract"),
              "properties" -> properties
            )
          }
        )
        //        val result = b execute "MATCH (c:Class) RETURN c" run
        //        val nodes: Iterator[Node] = result.columnAs[Node]("c")
        //
        //        JsArray(
        //          nodes.toArray.map({ n =>
        //            Json.obj(
        //              "fqcn" -> n.property[String]("fqcn")
        //            )
        //          })
        //        )
      }
    }
    future map { json => Ok(json) }
  }

}
