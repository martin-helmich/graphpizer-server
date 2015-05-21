package persistence

import org.neo4j.graphdb.{Result, GraphDatabaseService}
import play.api.Logger

import scala.collection.JavaConversions._

class Statement(graph: GraphDatabaseService, cypher: String) {

  val logger = Logger("cypher-statement")

  def run(): Result = {
    logger.info(s"Executing cypher statement $cypher")
    val result = graph.execute(cypher)
    logger.info(s"Done")
    result
  }

  def runWith(params: Map[String, AnyRef]): Result = graph.execute(cypher, mapAsJavaMap[String, AnyRef](params))

  def foreach[X](m: (X) => Unit): Unit = {
    val result = run()
    val columns = result.columns()
    try {
      while (result.hasNext) {
        val row = result.next()

        val p = row.get(columns(0)).asInstanceOf[X]
        m(p)
      }
    } finally {
      result.close()
    }
  }

  def foreach[X, Y](m: (X, Y) => Unit): Unit = {
    val result = run()
    val columns = result.columns()
    try {
      while (result.hasNext) {
        val row = result.next()

        val x = row.get(columns(0)).asInstanceOf[X]
        val y = row.get(columns(1)).asInstanceOf[Y]
        m(x, y)
      }
    } finally {
      result.close()
    }
  }

  def foreach[X, Y, Z](m: (X, Y, Z) => Unit): Unit = {
    val result = run()
    val columns = result.columns()
    try {
      while (result.hasNext) {
        val row = result.next()

        val x = row.get(columns(0)).asInstanceOf[X]
        val y = row.get(columns(1)).asInstanceOf[Y]
        val z = row.get(columns(2)).asInstanceOf[Z]
        m(x, y, z)
      }
    } finally {
      result.close()
    }
  }

}
