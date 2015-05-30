package domain.modelgeneration

import domain.modelgeneration.Generator.RunOptions
import domain.modelgeneration.typeinference.TypeInferer

class Generator(namespaceResolver: NamespaceResolver,
                classResolver: ClassResolver,
                typeInferer: TypeInferer,
                usageAnalyzer: UsageAnalyzer) {

  def run(options: RunOptions) = {
    namespaceResolver.run()
    classResolver.run()

    if (options.withTypeInference) {
      typeInferer.run()
    }

    if (options.withUsage) {
      usageAnalyzer.run()
    }
  }

}

object Generator {

  case class RunOptions(withUsage: Boolean, withTypeInference: Boolean)

}