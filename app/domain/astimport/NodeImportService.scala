package domain.astimport

import akka.actor.{Props, ActorLogging, Actor}
import akka.event.LoggingReceive
import controllers.dto.{Edge, Node, ImportDataSet}
import domain.astimport.NodeImportService.{WipeRequest, ImportRequest}
import org.neo4j.graphdb
import org.neo4j.graphdb._
import persistence.{ConnectionManager, Backend}
import scala.collection.JavaConversions._
import persistence.NodeWrappers._

class NodeImportService(manager: ConnectionManager) extends Actor with ActorLogging {

  def receive = LoggingReceive {
    case ImportRequest(project, data) => importData(project, data)
    case WipeRequest(project) => wipe(project)
  }

  def wipe = (project: String) => {
    manager connect project transactional { (b, t) =>
      b execute "MATCH (c) OPTIONAL MATCH (c)-[r]->() DELETE c, r" run
    }
  }

  def importData = (project: String, data: ImportDataSet) => {
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

//        val rel = start createRelationshipTo(end, b createEdgeType dto.label)
        dto.properties.foreach { case (key, value) => rel(key) = value }
      }

      log.info("Imported " + knownNodes.size + " nodes")
    }
  }

}

object NodeImportService {

  case class ImportRequest(project: String, data: ImportDataSet)

  case class WipeRequest(project: String)

}