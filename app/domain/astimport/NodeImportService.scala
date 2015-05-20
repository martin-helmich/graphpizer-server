package domain.astimport

import akka.actor.{Props, ActorLogging, Actor}
import controllers.dto.{Edge, Node, ImportDataSet}
import org.neo4j.graphdb.DynamicRelationshipType
import persistence.{ConnectionManager, Backend}

class NodeImportService extends Actor with ActorLogging {

  case class ImportRequest(project: String, data: ImportDataSet)

  val conn = ConnectionManager

//  val nodeImporters = context.actorOf(Props(classOf[NodeCreator], backend), "create-node")

  def receive = {
    case ImportRequest(project, data) =>
      log.info("Received import request")
      importData(project, data)
      log.info("Done")
  }

  def importData = (project: String, data: ImportDataSet) => {
    conn connect project transactional { (b, t) =>
      log.info("Fuck off, fucking Neo4j!?")
      val knownNodes = data.nodes.map { dto =>
        val node = b.createNode
        val id = dto.id

        dto.labels.foreach { l => node.addLabel(b createLabel l) }
        dto.properties.foreach { case (key, value) => node.setProperty(key, value) }

        (id, node)
      }.toMap

      data.edges.foreach { dto =>
        val start = knownNodes(dto.from)
        val end = knownNodes(dto.to)

        val rel = start createRelationshipTo(end, DynamicRelationshipType withName dto.label)
        dto.properties.foreach { case (key, value) => rel setProperty(key, value)}
        log.debug("Created relationship from " + dto.from + " to " + dto.to)
      }

      log.info("Imported " + knownNodes.size + " nodes")
    }
  }

}
