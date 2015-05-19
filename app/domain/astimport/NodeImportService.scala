package domain.astimport

import akka.actor.{Props, ActorLogging, Actor}
import controllers.dto.{Edge, Node, ImportRequest}
import org.neo4j.graphdb.DynamicRelationshipType
import persistence.Backend

class NodeImportService(backend: Backend) extends Actor with ActorLogging {

//  val nodeImporters = context.actorOf(Props(classOf[NodeCreator], backend), "create-node")

  def receive = {
    case ImportRequest(nodes, edges) =>
      log.info("Received import request")
      importData(nodes, edges)
      log.info("Done")
  }

  def importData = (dtos: Seq[Node], edgeDtos: Seq[Edge]) => {
    backend transactional { t =>
      val knownNodes = dtos.map { dto =>
        val node = backend.createNode
        val id = dto.id

        dto.labels.foreach { l => node.addLabel(backend createLabel l) }
        dto.properties.foreach { case (key, value) => node.setProperty(key, value) }

        log.debug("Imported node " + dto.labels + ", id " + id)
        (id, node)
      }.toMap

      edgeDtos.foreach { dto =>
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
