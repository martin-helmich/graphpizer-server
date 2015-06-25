package views.cypher

import org.neo4j.graphdb.Result

trait ResultView {

  def apply(result: Result, columns: Seq[String]): AnyRef

}
