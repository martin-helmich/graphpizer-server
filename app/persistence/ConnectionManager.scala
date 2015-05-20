package persistence

import org.neo4j.graphdb.factory.GraphDatabaseFactory
import scala.collection.mutable

object ConnectionManager {

  val basePath = "/tmp/graphizer"
  val connections = mutable.Map[String, BackendInterface]()
  val factory = new GraphDatabaseFactory()

  def connect(name: String): BackendInterface = {
    connections get name match {
      case Some(backend) => backend
      case _ =>
        synchronized {
          connections get name match {
            case Some(b) => b
            case _ =>
              val conn = factory.newEmbeddedDatabase(basePath + "/" + name)
              val backend = new Backend(conn)

              connections += name -> backend
              backend
          }
        }
    }
  }

  def shutdown() {
    connections.foreach { case (_, b) => b.shutdown() }
  }
}
