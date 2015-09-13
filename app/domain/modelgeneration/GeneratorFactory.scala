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

import javax.inject.{Inject, Singleton}

import domain.astimport.DocCommentParser
import domain.mapper.TypeMapper
import domain.model.Project
import domain.modelgeneration.typeinference.TypeInferer
import persistence.ConnectionManager

@Singleton
class GeneratorFactory @Inject()(manager: ConnectionManager, docCommentParser: DocCommentParser, typeResolver: TypeResolver, typeMapper: TypeMapper) {

  def forProject(project: Project): Generator = {
    val backend = manager connect project.slug
    new Generator(
      new NamespaceResolver(backend),
      new ClassResolver(backend, docCommentParser, typeResolver),
      new TypeInferer(backend, typeMapper, project),
      new UsageAnalyzer(backend)
    )
  }
}
