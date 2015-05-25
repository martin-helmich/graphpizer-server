package domain.model

case class Class(name: String,
                 namespace: Option[String],
                 isAbstract: Boolean = false,
                 isFinal: Boolean = false,
                 properties: Seq[Property] = Seq()) {

}

object Class {

  class ClassBuilder {
    val name: String = ""
    val namespace: Option[String] = None
    val isAbstract: Boolean = false
    val isFinal: Boolean = false
    val properties: Seq[Property] = Seq()

    def build(): Class = {
      Class(name, namespace, isAbstract, isFinal, properties = properties)
    }
  }

}