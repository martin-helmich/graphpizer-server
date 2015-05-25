package domain.astimport

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