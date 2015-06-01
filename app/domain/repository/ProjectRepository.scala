package domain.repository

import java.sql.Connection
import java.util.UUID

import domain.model.{Snapshot, Project}
import anorm._
import domain.repository.ProjectRepository.ProjectQuery
import org.joda.time.Instant
import persistence.LazyCollection
import play.api.db.DB
import play.api.Play.current

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class ProjectRepository {

  def all(implicit ctx: ExecutionContext): Future[Seq[Project]] = {
    Future {
      DB.withConnection { implicit c =>
        val selectAll = SQL("SELECT name, slug FROM projects")
        val res = selectAll().map { mapResult }

        res.toList
      }
    }
  }

  def findBySlug(slug: String)(implicit ctx: ExecutionContext): Future[Option[Project]] = {
    findOneBy(new ProjectQuery(slug = slug))
  }

  def findOneBy(q: ProjectQuery)(implicit ctx: ExecutionContext): Future[Option[Project]] = {
    Future {
      DB.withConnection { implicit c =>
        val (constraint, params) = q.toSql
        println(constraint, params)
        val sql = SQL(s"SELECT name, slug FROM projects WHERE $constraint LIMIT 1").on(params: _*)
        val res = sql().map { mapResult }

        res.headOption
      }
    }
  }

  def add(p: Project)(implicit ctx: ExecutionContext): Future[Boolean] = {
    Future {
      DB.withConnection { implicit c =>
        SQL"INSERT INTO projects (name, slug) VALUES (${p.name }, ${p.slug })".execute()
      }
    }
  }

  def update(p: Project)(implicit ctx: ExecutionContext): Future[Boolean] = {
    Future {
      DB.withConnection { implicit c =>
        SQL"UPDATE projects SET name=${p.name } WHERE slug=${p.slug }".execute()
        p.snapshots match {
          case snaps: LazyCollection[Snapshot] =>
            snaps.added foreach { s =>
              println("insert " + s)
              SQL"INSERT INTO snapshots (id, project, timestamp, size) VALUES (${s.id}, ${p.slug}, ${s.timestamp.getMillis}, ${s.size}})".execute()
            }
          case _ =>
        }
        true
      }
    }
  }

  def delete(p: Project)(implicit ctx: ExecutionContext): Future[Boolean] = {
    Future {
      DB.withConnection { implicit c => SQL"DELETE FROM projects WHERE slug=${p.slug }".execute() }
    }
  }

  protected def mapResult(r: Row)(implicit c: Connection): Project = {
    r match {
      case Row(name: String, slug: String) =>
        val snapshots = new LazyCollection[Snapshot]({ loadSnapshots(slug) })
        new Project(slug, name, snapshots)
    }
  }

  protected def loadSnapshots(s: String)(implicit c: Connection): Seq[Snapshot] = {
    DB.withConnection { implicit c =>
      println(s"loading snapshots for $s")
      SQL"SELECT id, timestamp, size FROM snapshots WHERE project=$s"().map {
        case Row(id: String, tstamp: Long, size: Long) =>
          new Snapshot(UUID.fromString(id), new Instant(tstamp), size)
      }
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

      (c reduce { (a, b) => s"$a AND $b" }, p.toSeq)
    }
  }

}