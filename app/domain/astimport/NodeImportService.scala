package domain.astimport

import akka.actor.{Props, ActorLogging, Actor}
import akka.event.LoggingReceive
import controllers.dto.{Edge, Node, ImportDataSet}
import domain.astimport.NodeImportService.{WipeRequest, ImportRequest}
import org.neo4j.graphdb
import org.neo4j.graphdb.DynamicRelationshipType
import persistence.{ConnectionManager, Backend}
import scala.collection.JavaConversions._

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
        if (dto.merge getOrElse false) {
          val params = Map("__node_id" -> dto.id)
          val res = b execute "MERGE (n {__node_id: {node}} RETURN n" runWith params

          val nodes: Iterator[graphdb.Node] = res.columnAs[graphdb.Node]("n")
//          nodes map { node =>  } TODO: Continue here!
        }

        val node = b.createNode
        val id = dto.id

        dto.labels.foreach { l => node.addLabel(b createLabel l) }
        dto.properties.foreach { case (key, value) => node.setProperty(key, value) }

        (id, node)
      }.toMap

      data.edges.foreach { dto =>
        val start = knownNodes(dto.from)
        val end = knownNodes(dto.to)

        val rel = start createRelationshipTo(end, b createEdgeType dto.label)
        dto.properties.foreach { case (key, value) => rel.setProperty(key, value) }
      }

      log.info("Imported " + knownNodes.size + " nodes")
    }
  }

}

object NodeImportService {

  case class ImportRequest(project: String, data: ImportDataSet)

  case class WipeRequest(project: String)

}