package persistence

import javax.inject.{Inject, Singleton}

import com.typesafe.config.Config
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import play.api.Logger
import scala.collection.mutable

@Singleton
class ConnectionManager @Inject() (config: Config) (factory: GraphDatabaseFactory) {

  protected val basePath = config getString "graphizer.datapath"
  protected val connections = mutable.Map[String, BackendInterface]()
  protected val logger = Logger("connection-manager")

  def connect(name: String): BackendInterface = {
    connections get name match {
      case Some(backend) => backend
      case _ =>
        synchronized {
          connections get name match {
            case Some(b) => b
            case _ =>
              logger.info("Connecting to database " + name)
              val conn = factory.newEmbeddedDatabase(basePath + "/" + name)
              val backend = new Backend(conn)
              logger.info("Done")

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
