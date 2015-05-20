package persistence

import javax.inject.{Inject, Singleton}

import org.neo4j.graphdb.factory.GraphDatabaseFactory
import scala.collection.mutable

@Singleton
class ConnectionManager @Inject() (factory: GraphDatabaseFactory) {

  protected val basePath = "/tmp/graphizer"
  protected val connections = mutable.Map[String, BackendInterface]()

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
