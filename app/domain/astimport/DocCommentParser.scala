package domain.astimport

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

import domain.astimport.DocComment.{VarTag, ReturnTag, ParamTag, Tag}
import scala.util.control.Breaks._

class DocCommentParser {

  protected val typePattern = """([a-zA-Z0-9_\\]+(?:\[\]|<[a-zA-Z0-9_\\]+>)?)"""

  protected val genericCommentPattern = "@([a-zA-Z0-9]+)\\s+(.*)".r
  protected val paramCommentPattern = ("""@param\s+""" + typePattern + """\s+\$?([a-zA-Z0-9_]+)\s*(.*)""").r
  protected val returnCommentPattern = ("""@return\s+""" + typePattern + """\s*(.*)""").r
  protected val varCommentPattern = ("""@var\s+""" + typePattern + """\s*(.*)""").r

  def parse(contents: String): DocComment = {
    val processed = cleanup(contents)

    val tags = genericCommentPattern findAllMatchIn processed filter { _ group 1 match {
      case "return"|"param" => false
      case _ => true
    }} map { m =>
      Tag(m group 1, m group 2)
    }

    val params = paramCommentPattern findAllMatchIn processed map { m =>
      ParamTag(m group 2, m group 1, m group 3)
    } map { t => (t.variable, t) } toMap

    val resultTags = returnCommentPattern findAllMatchIn processed map { m =>
      ReturnTag(m group 1, m group 2)
    } toSeq

    val returnTag = if (resultTags.nonEmpty) Option(resultTags.head) else None

    val varTags = varCommentPattern findAllMatchIn processed map { m =>
      VarTag(m group 1, m group 2)
    } toSeq

    val varTag = if (varTags.nonEmpty) Option(varTags.head) else None

    new DocComment(tags = tags.toSeq, paramTags = params, returnTag = returnTag, varTag = varTag)
  }

  protected def cleanup(contents: String): String = {
    val processLine = (l: String) => {
      var processed = l
      processed = "^\\s*/\\*\\*\\s*".r.replaceAllIn(processed, "")
      processed = "\\s*\\*/\\s*$".r.replaceAllIn(processed, "")
      processed = "^\\s*\\*\\s*".r.replaceAllIn(processed, "")
      processed
    }

    val processed = contents split "\n" map { processLine } mkString "\n"
    """^\s*""".r.replaceFirstIn("""\s*$""".r.replaceFirstIn(processed, ""), "")
  }

}
