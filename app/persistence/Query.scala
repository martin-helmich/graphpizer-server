package persistence

import org.neo4j.graphdb.Label

case class Query(label: Label, properties: Map[String, AnyRef] = null) {
}
