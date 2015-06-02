package domain.repository

import java.util.UUID

import akka.actor.Actor
import domain.model.Snapshot
import org.joda.time.Instant
import play.api.db.DB
import play.api.Play.current
import anorm._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

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
