package org.dgraph.actors

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import org.dgraph.db.DBManager
import org.dgraph.models.{DbActions, DbCreation, DbNode, Edge, FetchUidsResponse, NodeKey}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object DgraphActor{
  def apply(config: Config)(implicit ec: ExecutionContext) = Props(new DgraphActor(config)(ec))
}

class DgraphActor(config: Config)(implicit ec: ExecutionContext)  extends Actor {

  implicit val as: ActorSystem = context.system
  implicit val materializer = ActorMaterializer()

  val dbManager = DBManager(config, as)(ec)

  def queries(db: Seq[DbCreation]): Seq[NodeKey] = getEntityNodes(db)

  private def getEntityNodes(db: Seq[DbCreation]) = {
    db.flatMap(d => extractNodes(d.entities) ++ extractEdges(d.edges))
  }

  private def extractNodes(entities: Option[Seq[DbNode]]) = {
    val edgeNames = scala.collection.mutable.Set[String]()
    def incEdgeNo(edgeName: String) = {
      edgeNames += edgeName
      edgeNames.size
    }
    entities match {
      case Some(entities) => {
        entities.flatMap(n => {
          NodeKey(n.code, n.types.take(1))
          n.edges match {
            case Some(edges) => edges.map(edge => NodeKey(n.code, n.types.take(1), Some(edge.edgeName), Some(edge.code), Some(edge.types), incEdgeNo(edge.edgeName)))
            case None => Seq.empty
          }
        })
      }
      case None => Seq.empty
    }
  }

  private def extractEdges(edges: Option[Seq[Edge]]) = {
    edges match {
      case Some(edges) => edges.flatMap(e => Seq(
        NodeKey(e.fromCode, Seq(e.fromType)),
        NodeKey(e.toCode, Seq(e.toType))
      ))
      case None => Seq.empty
    }
  }

  private def findUid(code: String, res: FetchUidsResponse) = {
    res.q.find(n => n.code == code) match {
      case Some(uid) => s"<${uid.uid}>"
      case None => s"_:${fixSpecialChars(code)}"
    }
  }

  private def fixSpecialChars(s: String) =
    s.replaceAll("/","")
      .replaceAll("=","")
      .replaceAll("_","")

  private def findEmbededUid(code: String, embededCode: String, no: Int, res: FetchUidsResponse) = {
    val fixedCode = fixSpecialChars(embededCode)
    res.q.find(n => n.code == code) match {
      case Some(uid) => {
        uid.edge match {
          case Some(edge) if edge.nonEmpty => s"<${edge.head.uid}>"
          case _ => s"_:$fixedCode"
        }
      }
      case None => s"_:$fixedCode"
    }
  }


  def receive = {
    case dbAction: DbActions => {
      val senderInstance = sender()
      val mapping = scala.collection.mutable.Map[String, String]()
      val nodes = queries(dbAction.actions)
      dbManager.queryNodes(nodes) onComplete {
        case Success(res) => {
          println(res)
          nodes.foreach(n => {
            mapping.getOrElseUpdate(n.code, findUid(n.code, res))
            if(n.embededCode.nonEmpty){
              mapping.getOrElseUpdate(n.embededCode.get, findEmbededUid(n.code, n.embededCode.get, n.edgeNo, res))
            }
          })
          println(mapping)
          dbManager.executeMutations(dbAction.actions, mapping)
        }
        case Failure(ex) => throw ex
      }
    }
  }
}
