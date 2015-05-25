package domain.modelgeneration

import javax.inject.{Inject, Singleton}

import domain.astimport.DocCommentParser
import persistence.ConnectionManager

@Singleton
class GeneratorFactory @Inject() (manager: ConnectionManager, docCommentParser: DocCommentParser, typeResolver: TypeResolver) {

  def forProject(name: String): Generator = {
    val backend = manager connect name
    new Generator(
      new NamespaceResolver(backend),
      new ClassResolver(backend, docCommentParser, typeResolver)
    )
  }
}
