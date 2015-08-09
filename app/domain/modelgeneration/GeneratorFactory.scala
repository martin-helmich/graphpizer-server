package domain.modelgeneration

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
