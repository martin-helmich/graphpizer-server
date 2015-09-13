/**
 * GraPHPizer source code analytics engine
 * Copyright (C) 2015  Martin Helmich <kontakt@martin-helmich.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
