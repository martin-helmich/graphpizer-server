package controllers.dto

case class Node(labels: Seq[String], properties: Map[String, AnyRef], merge: Option[Boolean]) {
  def id = properties("__node_id").asInstanceOf[String]
}
case class Edge(from: String, to: String, label: String, properties: Map[String, AnyRef])
case class ImportRequest(nodes: Seq[Node], edges: Seq[Edge])