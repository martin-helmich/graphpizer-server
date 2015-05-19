import persistence.Backend
import play.api.{Logger, GlobalSettings}

object Global extends GlobalSettings {

  def onStop(app: App): Unit = {
    Logger.info("Application shutdown")
    Backend.shutdown
  }

}
