package util

import akka.actor.ActorLogging

trait WrappingActorLogging {
  this: ActorLogging =>

  sealed trait LogWrapper {
    def exec[T](m: => T)
  }

  def withLog(msg: String): LogWrapper = new LogWrapper {
    override def exec[T](m: => T): Unit = {
      log.info(msg)
      m
      log.info("Done")
    }
  }

}
