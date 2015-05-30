package domain.repository

import domain.model.Project
import anorm._
import domain.repository.ProjectRepository.ProjectQuery
import play.api.db.DB
import play.api.Play.current

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class ProjectRepository {
  
  def all(implicit ctx: ExecutionContext): Future[Seq[Project]] = {
    Future {
      DB.withConnection { implicit c =>
        val selectAll = SQL("SELECT name, slug FROM projects")
        val res = selectAll().map { case Row(name: String, slug: String) =>
          new Project(slug, name)
        }

        res.toList
      }
    }
  }

  def findOneBy(q: ProjectQuery)(implicit ctx: ExecutionContext): Future[Option[Project]] = {
    Future {
      DB.withConnection { implicit c =>
        val (constraint, params) = q.toSql
        println(constraint, params)
        val sql = SQL(s"SELECT name, slug FROM projects WHERE $constraint LIMIT 1").on(params:_*)
        val res = sql().map { case Row(name: String, slug: String) => new Project(slug, name) }
        
        res.headOption
      }
    }
  }

  def add(p: Project)(implicit ctx: ExecutionContext): Future[Boolean] = {
    Future {
      DB.withConnection { implicit c =>
        SQL"INSERT INTO projects (name, slug) VALUES (${p.name}, ${p.slug})".execute()
      }
    }
  }

  def update(p: Project)(implicit ctx: ExecutionContext): Future[Boolean] = {
    Future {
      DB.withConnection { implicit c =>
        SQL"UPDATE projects SET name=${p.name} WHERE slug=${p.slug}".execute()
      }
    }
  }

  def delete(p: Project)(implicit ctx: ExecutionContext): Future[Boolean] = {
    Future {
      DB.withConnection { implicit c => SQL"DELETE FROM projects WHERE slug=${p.slug}".execute() }
    }
  }
  
}

object ProjectRepository {
  class ProjectQuery(name: String = null, slug: String = null) {
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

      (c reduce { (a,b) => s"$a AND $b"}, p.toSeq)
    }
  }
}