package org.dgraph.db

import org.dgraph.models.NodeKey

object DgraphQueries {

  private def filterTypes(types: Seq[String]) = types.map(s =>
    s"""type($s)""".stripMargin).mkString(" or ")

  private def filterByCodeAndType(node: Seq[NodeKey]) = {
    val filters = scala.collection.mutable.Set[String]()
    val edges = scala.collection.mutable.Set[String]()
    node.foreach(n => {
      filters += s"""(eq(code, "${n.code}") and (${filterTypes(n.types)}))""".stripMargin
      if(n.edgeName.nonEmpty) {
        edges +=
          s"""edge: ${n.edgeName.get} {
             |uid
             |code
             |}
             |""".stripMargin
      }
    })
    (filters, edges)
  }

  def nodesQuery(nodeKeys: Seq[NodeKey]) = {
    val (filters, edges) = filterByCodeAndType(nodeKeys)
    val s = s"""
       |{
       |  q(func: type(Identifier))
       |  @filter(
       |      ${filters.mkString("or")}
       |  ) {
       |    uid
       |    code
       |    ${edges.mkString}
       |  }
       |}""".stripMargin
    println(s)
    s
  }

}
