package domain.model

import java.util.UUID

import org.joda.time.Instant

case class Snapshot(id: UUID, timestamp: Instant, size: Long) {
}
