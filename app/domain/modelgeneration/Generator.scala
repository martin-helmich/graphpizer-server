package domain.modelgeneration

import domain.modelgeneration.Generator.RunOptions

class Generator(namespaceResolver: NamespaceResolver,
                classResolver: ClassResolver) {

  def run(options: RunOptions) = {
    namespaceResolver.run()
    classResolver.run()
  }

}

object Generator {

  case class RunOptions(withUsage: Boolean, withTypeInference: Boolean)

}