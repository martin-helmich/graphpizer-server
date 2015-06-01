package domain.model

import scala.collection.mutable

case class Project(slug: String,
                   name: String,
                   snapshots: mutable.Buffer[Snapshot] = mutable.Buffer()) {

}
