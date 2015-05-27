package domain.modelgeneration.typeinference

import persistence.BackendInterface

class TypeInferencePass(backend: BackendInterface, symbols: SymbolTable) {

  protected var affected = -1
  protected var iter = 0

  def pass(): Unit = {

  }

  def affectedInLastPass: Int = { affected }

  def done: Boolean = { affected == 0 }

  def iterationCount: Int = { iter }

}
