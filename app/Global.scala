import persistence.{ConnectionManager, Backend}
import play.api._

object Global extends GlobalSettings {

  override def onStop(app: Application): Unit = {
    Logger.info("Application shutdown")
    ConnectionManager.shutdown()
  }

}
