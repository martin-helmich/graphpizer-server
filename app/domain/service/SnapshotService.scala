package domain.service

import akka.actor.Actor
import akka.actor.Actor.Receive
import domain.model.Snapshot
import domain.service.SnapshotService.CreateSnapshot

class SnapshotService extends Actor {

  def receive = {
    case CreateSnapshot(s) =>
  }

}

object SnapshotService {

  case class CreateSnapshot(project: String)

  case class CreateSnapshotSuccess(snapshot: Snapshot)
  case class CreateSnapshotError(err: Exception)

}