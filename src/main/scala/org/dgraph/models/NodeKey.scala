package org.dgraph.models

case class NodeKey(code: String, types: Seq[String], edgeName: Option[String] = None, embededCode: Option[String] = None, embededTypes: Option[Seq[String]] = None, edgeNo: Int = 0)
