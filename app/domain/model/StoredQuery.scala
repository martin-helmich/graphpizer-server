package domain.model

import java.util.UUID

case class StoredQuery(id: UUID, cypher: String)
