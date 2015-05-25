package domain.model

import domain.model.Property.Visibility.Visibility

case class Property(name: String,
                    visibility: Visibility,
                    static: Boolean = false,
                    possibleTypes: Seq[DataType] = Seq(),
                    docComment: String) {

}

object Property {

  object Visibility extends Enumeration {
    type Visibility = Value
    val Public, Protected, Private = Value
  }

}