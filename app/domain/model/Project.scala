package domain.model

case class Project(slug: String,
                   name: String,
                   snapshots: Seq[Snapshot] = Seq()) {

}
