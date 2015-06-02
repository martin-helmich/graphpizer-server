package util

import akka.actor.ActorRef

object AkkaHelpers {

  class FutureReply(sender: ActorRef) {
    def apply[T](x : (ActorRef) => T): T = {
      x(sender)
    }
  }

  object FutureReply {
    def apply(s: () => ActorRef): FutureReply = {
      new FutureReply(s())
    }
  }

}
