package domain.service

import javax.inject.Inject

import akka.actor.{Props, Actor}
import domain.model.Snapshot
import domain.repository.ProjectRepository

class SnapshotService extends Actor {
  import domain.service.SnapshotService._

  val projectRepository = context.actorOf(Props[ProjectRepository], "projects")

  def receive = {
    case CreateSnapshot(s) =>
    case GetSnapshots(s) =>
  }

}

object SnapshotService {

  case class CreateSnapshot(project: String)
  case class GetSnapshots(project: String)

  case class CreateSnapshotSuccess(snapshot: Snapshot)
  case class CreateSnapshotError(err: Exception)

}