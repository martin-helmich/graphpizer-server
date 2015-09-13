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
    case Row(id: UUID, cypher: Clob) => StoredQuery(id, cypher.getSubString(1, cypher.length().asInstanceOf[Int]))
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
