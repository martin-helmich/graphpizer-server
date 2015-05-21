package domain.modelgeneration

import javax.inject.{Inject, Singleton}

import persistence.ConnectionManager

@Singleton
class GeneratorFactory @Inject() (manager: ConnectionManager) {

  def forProject(name: String): Generator = {
    val backend = manager connect name
    new Generator(
      new NamespaceResolver(backend)
    )
  }
}
