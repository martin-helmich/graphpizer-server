package domain.repository

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

import java.util.UUID

import akka.actor.Actor
import anorm._
import domain.model.Snapshot
import org.joda.time.Instant
import play.api.Play.current
import play.api.db.DB
import util.AkkaHelpers._

class SnapshotRepository extends Actor {
  import SnapshotRepository._

  def receive = {
    case SnapshotById(id) => FutureReply(sender) { sender =>
      byId(id) match {
        case Some(s) => sender ! SnapshotResponse(s)
        case _ => sender ! SnapshotEmptyResponse()
      }
    }

    case SnapshotsByProject(project) => FutureReply(sender) { sender =>
      sender ! SnapshotsResponse(byProject(project))
    }
  }

  private def byId(id: UUID): Option[Snapshot] = {
    DB.withConnection { implicit c =>
      SQL"SELECT id, timestamp, size FROM snapshots WHERE id=$id"().map { mapResult }.headOption
    }
  }

  private def byProject(p: String): Seq[Snapshot] = {
    DB.withConnection { implicit c =>
      SQL"SELECT id, timestamp, size FROM snapshots WHERE project=$p"().map { mapResult }.toList
    }
  }

  private def mapResult(r: Row): Snapshot = {
    r match {
      case Row(id: UUID, tstamp: Long, size: Int) => new Snapshot(id, new Instant(tstamp), size)
    }
  }

}

object SnapshotRepository {

//  case class SnapshotQuery(id: UUID = null, project: String = null)
  case class SnapshotById(id: UUID)
  case class SnapshotsByProject(project: String)

  case class SnapshotResponse(snapshot: Snapshot)
  case class SnapshotEmptyResponse()
  case class SnapshotsResponse(snapshots: Seq[Snapshot])

}
