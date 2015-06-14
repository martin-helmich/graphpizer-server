package util

import akka.actor.ActorRef

import scala.util.Failure

object AkkaHelpers {

  class FutureReply(sender: ActorRef) {
    def apply(x : (ActorRef) => Any): Unit = {
      try {
        x(sender)
      } catch {
        case e: Exception => sender ! Failure(e)
      }
    }
  }

  object FutureReply {
    def apply(s: () => ActorRef): FutureReply = {
      new FutureReply(s())
    }
  }

}
