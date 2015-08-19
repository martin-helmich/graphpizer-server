package domain.modelgeneration

import domain.modelgeneration.Generator.RunOptions
import domain.modelgeneration.typeinference.TypeInferer
import play.api.Logger

class Generator(namespaceResolver: NamespaceResolver,
                classResolver: ClassResolver,
                typeInferer: TypeInferer,
                usageAnalyzer: UsageAnalyzer) {

  def run(options: RunOptions) = {
    Logger.info("Run configuration: " + options)

    try {
      Logger.info("Starting namespace resolution")
      namespaceResolver.run()

      Logger.info("Starting class resolution")
      classResolver.run()

      if (options.withTypeInference) {
        Logger.info("Starting type inference")
        typeInferer.run()
      }

      if (options.withUsage) {
        Logger.info("Starting usage analysis")
        usageAnalyzer.run()
      }
    }
    catch {
      case e: Exception => Logger.error(e.getMessage, e)
    }
  }

}

object Generator {

  case class RunOptions(withUsage: Boolean, withTypeInference: Boolean)

}