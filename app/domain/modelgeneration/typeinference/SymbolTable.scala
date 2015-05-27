package domain.modelgeneration.typeinference

import domain.model.DataType

import scala.collection.mutable

class SymbolTable {

  protected val scopes: mutable.Map[String, SymbolTable] = mutable.Map()
  protected val symbols: mutable.Map[String, mutable.Set[DataType]] = mutable.Map()

  def scope(name: String): SymbolTable = {
    scopes get name match {
      case Some(s: SymbolTable) => s
      case _ =>
        val t = new SymbolTable()
        scopes += name -> t
        t
    }
  }

  def addSymbol(name: String, types: Seq[DataType]): Unit = {
    val set = setForSymbol(name)
    types foreach { set += _ }
  }

  def addTypeForSymbol(name: String, typ: DataType): Unit = {
    setForSymbol(name) += typ
  }

  def hasSymbol(name: String): Boolean = {
    symbols get name match {
      case Some(seq: mutable.Set[DataType]) => true
      case _ => false
    }
  }

  def typesForSymbol(name: String): Set[DataType] = {
    symbols get name match {
      case Some(seq: mutable.Set[DataType]) => seq.toSet
      case _ => Set()
    }
  }

  protected def setForSymbol(name: String): mutable.Set[DataType] = {
    symbols get name match {
      case Some(set: mutable.Set[DataType]) => set
      case _ =>
        val set = mutable.Set[DataType]()
        symbols += (name -> set)
        set
    }
  }

}
