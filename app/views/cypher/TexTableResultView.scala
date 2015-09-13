package views.cypher

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
