package domain.modelgeneration

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