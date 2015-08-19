package controllers

import java.io.ByteArrayOutputStream
import javax.inject.{Inject, Singleton}

import domain.model.ClassLike
import net.sourceforge.plantuml.SourceStringReader
import org.neo4j.graphdb.Node
import persistence.ConnectionManager
import persistence.NodeWrappers._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsArray, Json}
import play.api.mvc._
import views.plantuml.ClassDiagram

import scala.concurrent.Future

@Singleton
class Packages @Inject()(manager: ConnectionManager) extends Controller {

  def list(project: String) = Action.async {
    Future {
      manager connect project transactional { (b, _) =>
        val f = b execute "MATCH (p:Package)-[:CONTAINS_FILE]->(f) RETURN p, COUNT(f)" map { (pkg: Node, fileCount: Long) =>
          Json.obj(
            "name" -> pkg[String]("name").get,
            "fileCount" -> fileCount
          )
        }
        JsArray(f)
      }
    } map { j => Ok(j) }
  }

  def uml(project: String, pkg: String, format: String = "txt") = Action {
    manager connect project transactional { (b, _) =>
      val cypher = """MATCH (c)-[:DEFINED_IN]->()<-[:HAS|SUB*]-(:File)<-[:CONTAINS_FILE]-(p:Package {name: {pkg}}) WHERE c:Class OR c:Trait OR c:Interface RETURN c"""
      val params = Map("pkg" -> pkg)
      val classes = b execute cypher params params map { (classLike: Node) => ClassLike.fromNode(classLike) }

      val umlconfig = ClassDiagram.DisplayConfiguration(withAutoPackages = false, withPackages = true, withUsages = true, includeRelatedClasses = true)
      val umlcode = ClassDiagram(classes, umlconfig)

      format match {
        case "txt" => Ok(umlcode)
        case "png" =>
          val reader = new SourceStringReader(umlcode)
          val out = new ByteArrayOutputStream(16 * 1024)

          reader.generateImage(out)
          Ok(out.toByteArray).as("image/png")
        case _ => NotAcceptable(s"Format not acceptable: $format")
      }
    }
  }

}
