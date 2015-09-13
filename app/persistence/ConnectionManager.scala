package persistence

/**
 * GraPHPizer source code analytics engine
 * Copyright (C) 2015  Martin Helmich <kontakt@martin-helmich.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.nio.file.{StandardCopyOption, Paths, Files}
import java.util.UUID
import javax.inject.{Inject, Singleton}

import com.typesafe.config.Config
import domain.model.Snapshot
import org.apache.commons.io.FileUtils
import org.joda.time.Instant
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import play.api.Logger

import scala.collection.mutable

@Singleton
class ConnectionManager @Inject() (config: Config) (factory: GraphDatabaseFactory) {

  protected val basePath = config getString "graphizer.datapath"
  protected val snapshotPath = config getString "graphizer.snapshotpath"
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

  def snapshot(name: String): Snapshot = {
    synchronized {
      val uuid = UUID.randomUUID()
      var size = 0L

      println("fuck off")
      val backend = connect(name)
      println("shutting down")
      backend.shutdown()
      println("shut down")

      val path = Paths.get(basePath, name)
      val target = Paths.get(snapshotPath, uuid.toString)

      println("Copying " + path + " to " + target)
      FileUtils.copyDirectory(path.toFile, target.toFile)
      println("Calculating size")
      size = FileUtils.sizeOfDirectory(path.toFile)
      println("Done, size was " + size)

      val conn = factory.newEmbeddedDatabase(path.toString)
      val newBackend = new Backend(conn)

      connections(name) = backend

      new Snapshot(uuid, Instant.now(), size)
    }
  }
}
