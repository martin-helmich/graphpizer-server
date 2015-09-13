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

class DocComment(short: String = "",
                 long: String = "",
                 paramTags: Map[String, ParamTag] = Map(),
                 returnTag: Option[ReturnTag] = None,
                 varTag: Option[VarTag] = None,
                 tags: Seq[Tag] = Seq()) {

  def tags(name: String): Seq[Tag] = {
    tags filter { _.name == name }
  }

  def longDescription: String = {
    long
  }

  def shortDescription: String = {
    short
  }

  def params: Seq[ParamTag] = paramTags.toSeq map { case (_, v) => v }

  def param(name: String): Option[ParamTag] = paramTags get name

  def result: Option[ReturnTag] = returnTag

  def variable: Option[VarTag] = varTag

}

object DocComment {

  case class ParamTag(variable: String, dataType: String, description: String)

  case class ReturnTag(dataType: String, description: String)

  case class VarTag(dataType: String, description: String)

  case class Tag(name: String, value: String)

}