package domain.service

import javax.inject.Inject

import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import domain.model.Snapshot
import domain.repository.SnapshotRepository._
import domain.repository.{ProjectRepository, SnapshotRepository}
import persistence.ConnectionManager

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

import util.AkkaHelpers._

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