package views.plantuml

import domain.model._
import domain.model.ClassLike.Visibility
import domain.model.ClassLike.Visibility.Visibility

object ClassDiagram {

  def apply(classes: Seq[ClassLike], withPackages: Boolean = true): String = {
    "@startuml\n" +
      (if (withPackages) "namespaceSeparator _\n" else "") +
      (classes map { renderClassLike(_, withPackages) }).mkString("\n") +
      (classes map { renderRelationships }).mkString("\n") +
    "@enduml\n"
  }

  private def renderClassLike(classLike: ClassLike, withPackages: Boolean = true): String = {
    val sb = new StringBuilder()
    sb.append(classTag(classLike) + " " + classLike.fqcn("_") + " {\n")

    classLike match {
      case klass: Class => sb.append(klass.properties.map(renderProperty).mkString("\n") + "\n")
      case _ =>
    }

    sb.append(classLike.methods.map(renderMethod).mkString("\n") + "\n")

    sb.append("}\n")
    sb.toString()
  }

  private def renderRelationships(classLike: ClassLike): String = classLike match {
    case klass: Class =>
      klass.parent.map { parent => klass.fqcn("_") + " --|> " + parent.fqcn("_") + "\n" }.getOrElse("") +
      klass.implements.map { interface => klass.fqcn("_") + " ..|> " + interface.fqcn("_") }.mkString("\n") + "\n" +
      klass.usesTraits.map { usedTrait => klass.fqcn("_") + " ..|> " + usedTrait.fqcn("_") }.mkString("\n") + "\n" +
      klass.usedClasses.map { used => klass.fqcn("_") + " --> " + used.fqcn("_") }.mkString("\n") + "\n"
    case iface: Interface =>
      iface.parent.map { parent => iface.fqcn("_") + " --|> " + parent.fqcn("_") + "\n" }.getOrElse("")
    case _ => ""
  }

  private def renderProperty(prop: Property): String = {
    "    " +
      renderVisibility(prop.visibility) +
      " " + prop.name +
      (if (prop.possibleTypes.nonEmpty) " : " + prop.possibleTypes.map { t => t.name }.mkString("|") else "")
  }

  private def renderMethod(meth: Method): String = {
    "    " +
    renderVisibility(meth.visibility) +
    " " + meth.name + "(" +
    meth.parameters.map { param =>
      "$" + param.name + (if (param.possibleTypes.nonEmpty) {
        " : " + param.possibleTypes.map { t => t.name}.mkString("|")
      } else "")
    }.mkString(", ") + ")" + (if (meth.possibleReturnTypes.nonEmpty) {
      " : " + meth.possibleReturnTypes.map { t => t.name}.mkString("|")
    } else "")
  }

  private def renderVisibility(vis: Visibility): String = vis match {
    case Visibility.Private => "-"
    case Visibility.Protected => "#"
    case _ => "+"
  }

  private def classTag(classLike: ClassLike): String = classLike match {
    case x: Class if x.isAbstract => "abstract class"
    case i: Interface => "interface"
    case _ => "class"
  }

}
