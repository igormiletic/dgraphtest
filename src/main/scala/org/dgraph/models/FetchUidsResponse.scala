package org.dgraph.models

case class UidNode2(uid: String, code: String)
case class UidNode(uid: String, code: String, edge: Option[Seq[UidNode2]] = None)
case class FetchUidsResponse(q: Seq[UidNode])
