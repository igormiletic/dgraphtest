package org.dgraph.actors

import akka.actor.{Actor, ActorRef}
import org.dgraph.models.{DbActions, DbCreation}

class ServiceActor(dgraphActor: ActorRef) extends Actor {

  var actions: scala.collection.mutable.Set[DbCreation] = scala.collection.mutable.Set.empty

  object Tick

  lazy val queueSize = 100

  def receive = {
    case dbAction: DbCreation => {
      val senderInstance = sender()
      actions += dbAction
      if(actions.size == queueSize) {
        self ! Tick
      }
      senderInstance ! "OK"
    }
    case Tick => {
      val actionsClone = actions.take(queueSize)
      actions = actions.drop(queueSize)
      dgraphActor ! DbActions(actionsClone.toSeq)
    }
  }
}
