import akka.actor.ActorSystem
import com.google.inject.{Provides, AbstractModule, Guice}
import com.typesafe.config.{Config, ConfigFactory}
import persistence.{ConnectionManager, Backend}
import play.api._

object Global extends GlobalSettings {

  val injector = Guice.createInjector(new AbstractModule {
    protected def configure(): Unit = {
      val config = ConfigFactory.load()

      this.bind(classOf[Config]).toInstance(config)
    }

    @Provides def provideActorSystem(): ActorSystem = {
      ActorSystem("graphpizer")
    }
  })

  override def getControllerInstance[A](controllerClass: Class[A]): A = injector.getInstance(controllerClass)

  override def onStop(app: Application): Unit = {
    Logger.info("Application shutdown")
    injector.getInstance(classOf[ConnectionManager]).shutdown()
  }

}
