package views.cypher

import java.text.NumberFormat
import java.util.Locale

import org.neo4j.graphdb.Result
import scala.collection.JavaConversions._

class TexTableResultView extends ResultView {

  val formatter = NumberFormat.getInstance(new Locale("de_DE"))

  implicit class TexString(s: String) {
    def toTex = {
      s.replace("_", "\\_")
    }
  }

  override def apply(result: Result, columns: Seq[String]): AnyRef = {
    val header = columns map { "\\textbf{" + _.toTex + "}" } mkString " & "
    val rows = result map { cells =>
      columns map { c => cells get c match {
        case s: java.lang.String => s.toTex
        case i: java.lang.Long => formatter format i
        case i: Integer => formatter format i
        case i: java.lang.Double => formatter format i
        case unknown: Object => unknown.getClass
        case null => "null"
      } } mkString " & "
    } mkString " \\\\\n"

    header + " \\\\\n" + rows
  }

}
