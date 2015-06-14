package domain.repository

import java.sql.Clob
import java.util.UUID

import akka.actor.{Actor, ActorLogging}
import anorm.{Row, SqlStringInterpolation}
import domain.model.StoredQuery
import play.api.Play.current
import play.api.db.DB
import util.AkkaHelpers.FutureReply

import scala.util.Success

class StoredQueryRepository extends Actor with ActorLogging {

  import StoredQueryRepository._

  def receive = {
    case AllStoredQueries() => FutureReply(sender) { sender => sender ! StoredQueriesResponse(all()) }
    case StoredQueryById(id) => FutureReply(sender) { sender =>
      sender ! (byId(id) map { StoredQueryResponse } getOrElse EmptyResponse())
    }
    case AddStoredQuery(q) => FutureReply(sender) { sender =>
      add(q)
      sender ! Success()
    }
    case UpdateStoredQuery(q) => FutureReply(sender) { sender =>
      update(q)
      sender ! Success()
    }
    case DeleteStoredQuery(q) => FutureReply(sender) { sender =>
      delete(q)
      sender ! Success()
    }
    case unknown => log.warning("Unknown message: %s", unknown)
  }

  protected def all(): Seq[StoredQuery] = DB.withConnection { implicit c =>
    SQL"SELECT id, cypher FROM queries"().map { mapResult }.toList
  }

  protected def byId(id: UUID): Option[StoredQuery] = DB.withConnection { implicit c =>
    SQL"SELECT id, cypher FROM queries WHERE id=$id"().map { mapResult }.headOption
  }

  protected def add(q: StoredQuery): Unit = DB.withConnection { implicit c =>
    SQL"INSERT INTO queries (id, cypher) VALUES (${q.id}, ${q.cypher})".executeInsert()
  }

  protected def update(q: StoredQuery): Unit = DB.withConnection { implicit c =>
    SQL"UPDATE queries SET cypher=${q.cypher} WHERE id=${q.id}".executeUpdate()
  }

  protected def delete(q: StoredQuery): Unit = DB.withConnection { implicit c =>
    SQL"DELETE FROM queries WHERE id=${q.id}".execute()
  }

  protected def mapResult(r: Row): StoredQuery = r match {
    case Row(id: UUID, cypher: Clob) => StoredQuery(id, cypher.toString)
  }

}

object StoredQueryRepository {

  case class AllStoredQueries()

  case class StoredQueryById(id: UUID)

  case class AddStoredQuery(query: StoredQuery)

  case class UpdateStoredQuery(query: StoredQuery)

  case class DeleteStoredQuery(query: StoredQuery)

  case class StoredQueriesResponse(queries: Seq[StoredQuery])
  case class StoredQueryResponse(query: StoredQuery)
  case class EmptyResponse()

}
