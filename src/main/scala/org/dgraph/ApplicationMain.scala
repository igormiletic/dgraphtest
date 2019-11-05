package org.dgraph

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Terminated
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.config.ConfigFactory
import org.dgraph.actors.{DgraphActor, ServiceActor}
import org.dgraph.service.MessagesService

import scala.concurrent.ExecutionContext

object ApplicationMain extends MessagesService{

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("Dgraph")

    implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))

    implicit val ex: ExecutionContext = system.dispatcher

    implicit val config = ConfigFactory.load("application.conf")

    val host = config.getString("http.interface")
    val port = config.getInt("http.port")

    val dgraphAcror = system.actorOf(DgraphActor.apply(config), "dgraphActor")
    val serviceActor = system.actorOf(Props(classOf[ServiceActor], dgraphAcror) ,"serviceActor")


    val apiRoutes = routes(serviceActor)

    Http().bindAndHandle(apiRoutes, host, port)

    system.actorOf(Props(classOf[Terminator], serviceActor), "terminator")
  }

  class Terminator(ref: ActorRef) extends Actor with ActorLogging {
    context watch ref
    def receive = {
      case Terminated(_) =>
        log.info("{} has terminated, shutting down system", ref.path)
        context.system.terminate()
    }
  }

}
