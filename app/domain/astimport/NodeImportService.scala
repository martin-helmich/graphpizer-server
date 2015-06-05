package domain.astimport

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import controllers.dto.ImportDataSet
import domain.astimport.NodeImportService.{ImportRequest, WipeRequest}
import org.neo4j.graphdb
import org.neo4j.graphdb._
import persistence.ConnectionManager
import persistence.NodeWrappers._
import util.WrappingActorLogging

import scala.collection.JavaConversions._

class NodeImportService(manager: ConnectionManager) extends Actor with WrappingActorLogging with ActorLogging {

  def receive = LoggingReceive {
    case ImportRequest(project, data) => importData(project, data)
    case WipeRequest(project) => wipe(project)
    case unknown => log.warning("Unknown request to actor: " + unknown)
  }

  def wipe = (project: String) => withLog(s"Wiping all nodes on project $project") exec {
    manager connect project transactional { (b, t) =>
      b.execute("MATCH (c) OPTIONAL MATCH (c)-[r]->() DELETE c, r").run()
    }
  }

  def importData = (project: String, data: ImportDataSet) => withLog(s"Importing ${data.nodes.size } nodes.") exec {
    manager connect project transactional { (b, t) =>
      val knownNodes = data.nodes.map { dto =>
        dto.merge match {
          case Some(true) =>
            val labels = dto.labels mkString ":"
            val properties = dto.properties map { case (key, value) => key + ": {node}." + key } mkString ", "
            val params = Map("node" -> dto.properties)

            val res = b execute s"MERGE (n:$labels  {$properties}) RETURN n" runWith params

            val nodes: Iterator[graphdb.Node] = res.columnAs[graphdb.Node]("n")
            val arr = nodes.toArray
            (dto.id, arr(0))
          case _ =>
            val node = b.createNode()

            dto.labels.foreach { l => node.addLabel(b createLabel l) }
            dto.properties.foreach { case (key, value) => node(key) = value }

            (dto.id, node)
        }
      }.toMap

      data.edges.foreach { dto =>
        val start = knownNodes(dto.from)
        val end = knownNodes(dto.to)

        val rel: Relationship = start --| dto.label |--> end <

        dto.properties.foreach { case (key, value) => rel(key) = value }
      }
    }
  }

}

object NodeImportService {

  case class ImportRequest(project: String, data: ImportDataSet)

  case class WipeRequest(project: String)

}