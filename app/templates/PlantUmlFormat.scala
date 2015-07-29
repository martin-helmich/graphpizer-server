package templates

import play.twirl.api.{Txt, Format}

import scala.collection.immutable.Seq

object PlantUmlFormat extends Format[Txt] {
  def raw(text: String) : Txt = Txt(text)
  def escape(text: String) : Txt = Txt(text)

  def empty: Txt = Txt("")

  override def fill(elements: Seq[Txt]): Txt = if (elements.length > 1) {
    elements reduce { (a, b) => Txt(a.body + b.body) }
  } else {
    elements.headOption.getOrElse(empty)
  }
}
