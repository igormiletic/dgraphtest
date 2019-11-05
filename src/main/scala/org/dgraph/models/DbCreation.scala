package org.dgraph.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include

case class NodeAttribute(name: String, value: String)
case class Edge(fromType: String, fromCode: String, toType: String, toCode: String, edgeName: String, @JsonInclude(Include.NON_ABSENT) facets: Option[Seq[NodeAttribute]] = None)
//case class EdgeNode(types: Seq[String], code: String, @JsonInclude(Include.NON_ABSENT) attributes: Option[Seq[NodeAttribute]] = None, edgeName: String, @JsonInclude(Include.NON_ABSENT) facets: Option[Seq[NodeAttribute]] = None, @JsonInclude(Include.NON_ABSENT) edges: Option[Seq[Edge]] = None, upsert: Boolean = false)
case class EdgeNode2(types: Seq[String], code: String, @JsonInclude(Include.NON_ABSENT) attributes: Option[Seq[NodeAttribute]] = None, edgeName: String, @JsonInclude(Include.NON_ABSENT) facets: Option[Seq[NodeAttribute]] = None, @JsonInclude(Include.NON_ABSENT) edges: Option[Seq[Edge]] = None, upsert: Boolean = false)
case class DbNode(types: Seq[String], code: String, @JsonInclude(Include.NON_ABSENT) attributes: Option[Seq[NodeAttribute]] = None, @JsonInclude(Include.NON_ABSENT) edges: Option[Seq[EdgeNode2]] = None)
case class DbCreation(ts: Long, @JsonInclude(Include.NON_ABSENT) entities: Option[Seq[DbNode]] = None, @JsonInclude(Include.NON_ABSENT) edges: Option[Seq[Edge]] = None)


