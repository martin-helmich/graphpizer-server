package domain.modelgeneration

import domain.modelgeneration.Generator.RunOptions
import domain.modelgeneration.typeinference.TypeInferer

class Generator(namespaceResolver: NamespaceResolver,
                classResolver: ClassResolver,
                typeInferer: TypeInferer) {

  def run(options: RunOptions) = {
    namespaceResolver.run()
    classResolver.run()

    if (options.withTypeInference) {
      typeInferer.run()
    }
  }

}

object Generator {

  case class RunOptions(withUsage: Boolean, withTypeInference: Boolean)

}