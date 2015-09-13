package domain.service

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

import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import domain.model.Snapshot
import domain.repository.SnapshotRepository._
import domain.repository.{ProjectRepository, SnapshotRepository}
import persistence.ConnectionManager
import util.AkkaHelpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SnapshotService(connectionManager: ConnectionManager) extends Actor {
  import domain.service.SnapshotService._

  val projects = context.actorOf(Props[ProjectRepository], "projects")
  val snapshots = context.actorOf(Props[SnapshotRepository], "snapshots")

  implicit val to = Timeout(1.second)

  def receive = {
    case CreateSnapshot(project) =>
      val s = connectionManager.snapshot(project)
      sender() ! CreateSnapshotSuccess(s)
    case SnapshotsByProject(project) => FutureReply(sender) { s =>
      snapshots ? SnapshotsByProject(project) map { s ! _ }
    }
  }

}

object SnapshotService {

  case class CreateSnapshot(project: String)

  case class CreateSnapshotSuccess(snapshot: Snapshot)
  case class CreateSnapshotError(err: Exception)

}