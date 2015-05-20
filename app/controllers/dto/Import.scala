package controllers.dto

import java.util.UUID

import play.api.Logger

case class Node(labels: Seq[String], properties: Map[String, Any], merge: Option[Boolean]) {

  val id = properties get "__node_id" match {
    case Some(i) => i.asInstanceOf[String]
    case _ =>
      Logger.warn("Node " + labels + " does not have an id")
      UUID.randomUUID().toString
  }

}
case class Edge(from: String, to: String, label: String, properties: Map[String, Any])
case class ImportDataSet(nodes: Seq[Node], edges: Seq[Edge])