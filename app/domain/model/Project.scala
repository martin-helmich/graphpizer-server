package domain.model

import domain.model.Project.AdditionalTransformation

case class Project(slug: String, name: String, additionalTransformations: Iterable[AdditionalTransformation])

object Project {

  case class AdditionalTransformation(when: String, cypher: String)

}