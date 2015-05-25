package domain.astimport

import domain.astimport.DocComment.{VarTag, ReturnTag, ParamTag}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class DocCommentParserSpec extends Specification {

  "A DocComment parser" should {
    val parser = new DocCommentParser

    "extract arbitrary tags" in {
      "when tag occurs once" in {
        val comment = parser parse "/**\n * @foo bar\n */"

        comment tags "foo" must have size 1

        val tag = (comment tags "foo").head
        tag.value must be equalTo "bar"
      }

      "with multiple same-named tags" in {
        val comment = parser parse """/**
                                     | * @foo bar
                                     | * @foo baz
                                     | */
                                   """.stripMargin

        comment tags "foo" must have size 2

        (comment tags "foo")(0).value must be equalTo "bar"
        (comment tags "foo")(1).value must be equalTo "baz"
      }

      "with multiple differently named tags" in {
        val comment = parser parse """/**
                                     | * @foo bar
                                     | * @bar baz
                                     | */
                                   """.stripMargin

        comment tags "foo" must have size 1
        comment tags "bar" must have size 1

        (comment tags "foo").head.value must be equalTo "bar"
        (comment tags "bar").head.value must be equalTo "baz"
      }
    }

    "extract param tags" in {
      "with variable and type name" in {
        val comment = parser parse """/**
                                     | * @param string $myString String parameter
                                     | */
                                   """.stripMargin

        comment.params must have size 1
        (comment param "myNonexitingString") must be none;

        comment param "myString" match {
          case Some(ParamTag("myString", "string", "String parameter")) => success
          case x => failure(x.toString)
        }
      }

      "with array type hint" in {
        val comment = parser parse """/**
                                     | * @param string[] $myStrings String array parameter
                                     | */
                                   """.stripMargin

        comment param "myStrings" match {
          case Some(ParamTag("myStrings", "string[]", _)) => success
          case x => failure(x.toString)
        }
      }
      "with generic type hint" in {
        val comment = parser parse """/**
                                     | * @param array<string> $myStrings String array parameter
                                     | */
                                   """.stripMargin

        comment param "myStrings" match {
          case Some(ParamTag("myStrings", "array<string>", _)) => success
          case x => failure(x.toString)
        }
      }
    }

    "extract return tags" in {
      "with type name" in {
        val comment = parser parse """/**
                                     | * @return string String return value
                                     | */""".stripMargin

        comment.result must be some

        comment.result match {
          case Some(ReturnTag("string", "String return value")) => success
          case x => failure(x.toString)
        }
      }

      "with array type name" in {
        val comment = parser parse """/**
                                     | * @return string[] String return value
                                     | */""".stripMargin

        comment.result must be some

        comment.result match {
          case Some(ReturnTag("string[]", "String return value")) => success
          case x => failure(x.toString)
        }
      }
      "with generic type name" in {
        val comment = parser parse """/**
                                     | * @return array<string> String return value
                                     | */""".stripMargin

        comment.result must be some

        comment.result match {
          case Some(ReturnTag("array<string>", "String return value")) => success
          case x => failure(x.toString)
        }
      }

      "optionally" in {
        val commentString =
          """/**
            | * @foo bar
            | */
          """.stripMargin

        (parser parse commentString).result must be none
      }
    }

    "extract var tags" in {
      "with type name" in {
        val commentString =
          """/**
            | * @var string String property
            | */
          """.stripMargin

        val comment = parser parse commentString

        comment.variable must be some

        comment.variable match {
          case Some(VarTag("string", "String property")) => success
          case x => failure(x.toString)
        }
      }

      "with array type name" in {
        val commentString =
          """/**
            | * @var string[] String array property
            | */
          """.stripMargin

        (parser parse commentString).variable match {
          case Some(VarTag("string[]", _)) => success
          case x => failure(x.toString)
        }
      }

      "with generic type name" in {
        val commentString =
          """/**
            | * @var array<string> String array property
            | */
          """.stripMargin

        (parser parse commentString).variable match {
          case Some(VarTag("array<string>", _)) => success
          case x => failure(x.toString)
        }
      }

      "optionally" in {
        val commentString =
          """/**
            | * @foo bar
            | */
          """.stripMargin

        val comment = parser parse commentString

        comment.variable must be none
      }
    }

  }

}
