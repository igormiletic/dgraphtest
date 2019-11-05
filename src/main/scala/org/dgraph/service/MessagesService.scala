package org.dgraph.service

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import akka.pattern.ask
import org.dgraph.models.{DbCreation, DbNode, Edge, EdgeNode2, NodeAttribute}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._
trait MessagesService extends SprayJsonSupport with DefaultJsonProtocol{
  implicit val timeout: Timeout = 5.seconds

  implicit val bivrostNodeAttribute = jsonFormat2(NodeAttribute)
  implicit val bivrostNodesEdge = jsonFormat6(Edge)
  implicit val bivrostEdgeNode = jsonFormat7(EdgeNode2)
  implicit val bivrostDbNode = jsonFormat4(DbNode)
  implicit val bivrostDbCreation = jsonFormat3(DbCreation)

  def routes(dgraphActor: ActorRef) = {
    route(dgraphActor)
  }
  def route(dgraphActor: ActorRef) = {
    path("createNode") {
      post {
        extractRequest { _ => {
          entity(as[DbCreation]) { createNodeRequest => {
            complete {
              StatusCodes.OK -> (dgraphActor ? createNodeRequest).mapTo[String]
            }
          }
          }
        }
        }
      }
    }
  }
}
