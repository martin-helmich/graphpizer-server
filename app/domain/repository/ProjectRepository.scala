package domain.repository

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

import java.sql.Connection

import akka.actor.Actor
import anorm._
import domain.model.Project
import domain.model.Project.AdditionalTransformation
import play.api.Logger
import play.api.Play.current
import play.api.db.DB
import util.AkkaHelpers._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ProjectRepository extends Actor {

  import ProjectRepository._

  def receive = {
    case ProjectQuery(null, null, _) => FutureReply(sender) { s =>
      s ! ProjectResponseSet(all)
    }

    case q: ProjectQuery if q.one => FutureReply(sender) { s =>
      val r = findOneBy(q) match {
        case Some(p) => ProjectResponse(p)
        case _ => ProjectEmptyResponse()
      }
      s ! r
    }

    case q: ProjectQuery => FutureReply(sender) { s =>
      s ! ProjectResponseSet(findBy(q))
    }

    case DeleteProject(p) => FutureReply(sender) { s =>
      try { {
        delete(p)
        s ! DeleteProjectSuccess()
      }
      }
      catch {
        case e: Exception => sender() ! DeleteProjectError(e)
      }
    }

    case DeleteProjectByQuery(q) => FutureReply(sender) { s =>
      val future = Future.sequence(findBy(q) map { p => Future {
        delete(p)
      }
      })
      future onSuccess { case _ => s ! DeleteProjectSuccess() }
      future onFailure { case e: Exception => s ! DeleteProjectError(e) }
    }

    case AddProject(p) => FutureReply(sender) { s =>
      add(p)
      s ! AddProjectSuccess()
    }

    case UpdateProject(p) => FutureReply(sender) { s =>
      update(p)
      s ! UpdateProjectSuccess()
    }

    case whut => Logger.warn("Strange message " + whut)
  }

  protected def all(implicit ctx: ExecutionContext): Seq[Project] = {
    DB.withConnection { implicit c =>
      SQL"SELECT name, slug FROM projects"().map {
        mapResult
      }.toList
    }
  }

  protected def findBySlug(slug: String)(implicit ctx: ExecutionContext): Option[Project] = {
    findOneBy(new ProjectQuery(slug = slug))
  }

  protected def findOneBy(q: ProjectQuery)(implicit ctx: ExecutionContext): Option[Project] = {
    DB.withConnection { implicit c =>
      val (constraint, params) = q.toSql
      val sql = SQL(s"SELECT name, slug FROM projects WHERE $constraint LIMIT 1").on(params: _*)()

      sql.map {
        mapResult
      }.headOption
    }
  }

  protected def findBy(q: ProjectQuery)(implicit ctx: ExecutionContext): Seq[Project] = {
    DB.withConnection { implicit c =>
      val (constraint, params) = q.toSql
      val sql = SQL(s"SELECT name, slug FROM projects WHERE $constraint").on(params: _*)()

      sql.map {
        mapResult
      }
    }
  }

  protected def add(p: Project)(implicit ctx: ExecutionContext): Boolean = {
    DB.withConnection { implicit c =>
      SQL"INSERT INTO projects (name, slug) VALUES (${p.name}, ${p.slug})".execute() &&
        p.additionalTransformations.map { t =>
          SQL"INSERT INTO additional_queries (project, cypher, when) VALUES (${p.slug}, ${t.cypher}, ${t.when})".execute()
        }.reduce((a, b) => a && b)
    }
  }

  protected def update(p: Project)(implicit ctx: ExecutionContext): Boolean = {
    DB.withConnection { implicit c =>
      SQL"UPDATE projects SET name=${p.name} WHERE slug=${p.slug}".execute()
      SQL"DELETE FROM additional_queries WHERE project=${p.slug}".execute()
      println("FUCK")
      println(p.additionalTransformations)
      try {
        p.additionalTransformations.foreach { t =>
          println(t)
          val s = SQL"INSERT INTO additional_queries (project, cypher, when) VALUES (${p.slug}, ${t.cypher}, ${t.when})"
          val r = s.execute()
          println(s)
          println(r)
        }
      } catch {
        case e: Exception => println(e)
      }
      true
    }
  }

  protected def delete(p: Project)(implicit ctx: ExecutionContext): Boolean = {
    DB.withConnection { implicit c => SQL"DELETE FROM projects WHERE slug=${p.slug}".execute() }
  }

  protected def mapResult(r: Row)(implicit c: Connection): Project = {
    r match {
      case Row(name: String, slug: String) =>
        val additional = SQL"SELECT cypher, when FROM additional_queries WHERE project=$slug"().map { case Row(cypher: String, when: String) =>
          new AdditionalTransformation(when, cypher)
        }.toList
        new Project(slug, name, additional)
    }
  }

}

object ProjectRepository {

  case class ProjectQuery(name: String = null, slug: String = null, one: Boolean = false) {
    def toSql: (String, Seq[NamedParameter]) = {
      val c = mutable.Buffer[String]("1")
      val p = mutable.Buffer[NamedParameter]()

      if (name != null) {
        c += "name={name}"
        p += "name" -> name
      }

      if (slug != null) {
        c += "slug={slug}"
        p += "slug" -> slug
      }

      (c reduce { (a, b) => s"$a AND $b" }, p.toSeq)
    }
  }

  case class DeleteProjectByQuery(query: ProjectQuery)

  case class DeleteProject(project: Project)

  case class DeleteProjectSuccess()

  case class DeleteProjectError(e: Exception)

  case class UpdateProject(project: Project)

  case class UpdateProjectSuccess()

  case class AddProject(project: Project)

  case class AddProjectSuccess()

  case class ProjectResponseSet(projects: Seq[Project])

  case class ProjectResponse(project: Project)

  case class ProjectEmptyResponse()

}