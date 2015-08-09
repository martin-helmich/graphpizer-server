package domain.model

import domain.model.Project.AdditionalTransformation

case class Project(slug: String, name: String, additionalTransformations: Seq[AdditionalTransformation]) {

  def additionalStmts(when: String): Seq[AdditionalTransformation] =
    additionalTransformations.filter { _.when == when }

}

object Project {

  case class AdditionalTransformation(when: String, cypher: String)

}