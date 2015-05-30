package domain.modelgeneration

import javax.inject.{Inject, Singleton}

import domain.astimport.DocCommentParser
import domain.mapper.TypeMapper
import domain.modelgeneration.typeinference.TypeInferer
import persistence.ConnectionManager

@Singleton
class GeneratorFactory @Inject()(manager: ConnectionManager, docCommentParser: DocCommentParser, typeResolver: TypeResolver, typeMapper: TypeMapper) {

  def forProject(name: String): Generator = {
    val backend = manager connect name
    new Generator(
      new NamespaceResolver(backend),
      new ClassResolver(backend, docCommentParser, typeResolver),
      new TypeInferer(backend, typeMapper),
      new UsageAnalyzer(backend)
    )
  }
}
