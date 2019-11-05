package org.dgraph.db

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.Collections
import java.util.concurrent.{ExecutionException, TimeUnit}

import akka.actor.ActorSystem
import com.google.protobuf.ByteString
import com.typesafe.config.Config
import io.dgraph.DgraphProto.{Mutation, Request}
import io.dgraph.{AsyncTransaction, DgraphAsyncClient, DgraphGrpc, TxnConflictException}
import io.grpc.{CallOptions, Channel, ClientCall, ClientInterceptor, ManagedChannel, ManagedChannelBuilder, MethodDescriptor}
import javax.naming.directory.{Attribute, InitialDirContext}
import org.dgraph.models.{DbCreation, DbNode, FetchUidsResponse, NodeKey}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Random, Success, Try}
import scala.collection.JavaConverters._
import scala.collection.mutable

object DBManager {
  def apply(config: Config, system: ActorSystem)(implicit ex: ExecutionContext): DBManager = new DBManager(config, system)(ex)
}

class DBManager(config: Config, system: ActorSystem)(implicit ex: ExecutionContext){

  val url = config.getString("dgraph.url")
  val port = config.getInt("dgraph.port")

  private class DgraphCallInterceptor extends ClientInterceptor(){
    override def interceptCall[ReqT, RespT](method: MethodDescriptor[ReqT, RespT], callOptions: CallOptions, next: Channel): ClientCall[ReqT, RespT] = {
      next.newCall(method, callOptions.withDeadlineAfter(1000, TimeUnit.MILLISECONDS))
    }
  }

  def createStub(dgraphUrl: String, dgraphPort: Int) = {
    val channel: ManagedChannel = ManagedChannelBuilder.forAddress(url, port).usePlaintext(true).build
    println(s"Creating stub $dgraphUrl :  $dgraphPort")
    val stub =  DgraphGrpc.newStub(channel)
    stub.withInterceptors(new DgraphCallInterceptor())
    stub
  }

  def createClient(url: String, port: Int, username: String, password: String) = {
    url match {
      case "localhost" =>  DBConnectionHolder(Seq(new DgraphAsyncClient(createStub(url, port))))
      case _ => {
        val attr: Attribute = new InitialDirContext().getAttributes(s"dns:$url", Array[String]("A")).get("A")
        val s = Collections.list(attr.getAll).asScala.map(_.toString).toSeq
        if(s.isEmpty) {
          println("DNS for dgraph nodes returned zero IP addresses.")
          system.terminate()
          System.exit(1)
        }

        val stubsList = for {
          _ <- 0 to s.size
          a <- createListOfStubs(port, Random.shuffle(s))
        } yield a

        DBConnectionHolder(stubsList)
      }
    }
  }

  private def createListOfStubs(port: Int, s: Seq[String]) = {
    val stubs = s.map(nodeUrl => createStub(nodeUrl.toString(), port))
    val randomStabs = Random.shuffle(stubs)
    val clients = for {_ <- 0 to randomStabs.size
                       cl = new DgraphAsyncClient(randomStabs: _*)
                       } yield cl
    clients
  }

  val clients = createClient(url, port, "", "")

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
  def currentTime(instant: Instant) = {

    //instant: java.time.Instant = 2017-02-13T12:14:20.666Z
    val zonedDateTimeUtc = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"))
    zonedDateTimeUtc.format(formatter)
  }

  def createMutation(ts: Long, key: String, code: String, types: Seq[String], attributes: Option[Map[String, String]]) = {
    val node = types.map(s => s"""$key <dgraph.type> "${s}" .""".stripMargin).toSet ++
      Set(s"""$key <code> "$code" .""".stripMargin) ++
      Seq(s"""$key <version> "${currentTime(Instant.ofEpochMilli(System.currentTimeMillis()))}" .""".stripMargin) ++
      Seq(s"""$key <ts> "${currentTime(Instant.ofEpochSecond(ts))}" .""".stripMargin)
    val attrs = attributes match {
      case Some(attrs) => attrs.map(s => s"""$key <${s._1}> "${s._2}" .""".stripMargin)
      case None => Set.empty
    }
    node ++ attrs
  }

  private def generateMutationInstructionsForNodes(ts: Long, mapping: mutable.Map[String, String], e: DbNode) = {
    val masterKey = mapping(e.code)
    val a = createMutation(ts, masterKey, e.code, e.types, Option(e.attributes.getOrElse(Seq.empty).map(b => b.name -> b.value).toMap))
    val b = e.edges match {
      case Some(emb) => {
        emb.flatMap(ed => {
          val childKey = mapping(ed.code)
          val e1 = createMutation(ts, childKey, ed.code, ed.types, Option(ed.attributes.getOrElse(Seq.empty).map(b => b.name -> b.value).toMap))
          val e2 = Set(s"$masterKey <${ed.edgeName}> $childKey .")
          e1 ++ e2
        })
      }
      case None => Set.empty
    }
    a ++ b

  }

  def executeMutations(creations: Seq[DbCreation], mapping: mutable.Map[String, String]) = {
    val ents =
      creations.flatMap(dbAction =>
        dbAction.entities match {
          case Some(entities) => {
            entities.flatMap(e => generateMutationInstructionsForNodes(dbAction.ts, mapping, e))
          }
          case None => Seq.empty
        })
    val edges = generateMutationInstructionsForEdges(creations, mapping)
    val instructions = ents.toSet ++ edges.toSet
    execMutation(instructions.mkString("\n"))
  }

  def execMutation(mutationBody: String) = {
    println(mutationBody)
    val transaction = clients.getClient().newTransaction()
    val p = Promise[Boolean]()
    try {
      val mu = Mutation.newBuilder().setSetNquads(ByteString.copyFromUtf8(mutationBody)).build()
      val req = Request.newBuilder().addMutations(mu).setCommitNow(true).build();
      mutation(Some(req), None,(result) => {
        result.nonEmpty match {
          case true =>  p.success(true)
          case false => p.success(false)
        }
      }, Some(transaction))
    }catch {
      case e: Exception =>
        e.printStackTrace()
        transaction.discard()
        throw e
    }
    finally {
      transaction.discard()
    }
    p.future
  }

  def mutation[T](rq: Option[Request] = None, mu: Option[Mutation] = None, response: mutable.Map[String, String] => T, txn: Option[AsyncTransaction] = None, retry: Int = 0): Future[T] = {
    val transaction = txn.getOrElse(clients.getClient().newTransaction())
    val p = Promise[T]()
    try {
      val r = rq.nonEmpty match {
        case true => transaction.doRequest(rq.get)
        case false => {
          // suppose one of request or mutation must have value
          transaction.mutate(mu.get)
        }
      }
      try {
        r.get() //verify all is good
        transaction.discard()
      }catch {
        case ee: ExecutionException  => retryMutation(rq, mu, response, retry, ee)
        case ee: TxnConflictException  => retryMutation(rq, mu, response, retry, ee)
        case ex: Exception =>
          throw ex
      }
      finally {
        transaction.discard()
      }
      r.thenAccept(s => p.success(response(s.getUidsMap.asScala)))
    }catch {
      case e: Exception =>
        p.failure(e)
        throw e
    }
    p.future
  }


  val maxRetry = 5
  private def retryMutation[T](rq: Option[Request], mu: Option[Mutation], response: mutable.Map[String, String] => T, retry: Int, ee: Exception) = {
    if (retry <= maxRetry) {
      println("Retry: " + retry)
      mutation(rq, mu, response, None, retry + 1)
    }
    else {
      ee.printStackTrace()
    }
  }


  private def generateMutationInstructionsForEdges(creations: Seq[DbCreation], mapping: mutable.Map[String, String]) = {
    creations.flatMap(dbAction =>
      dbAction.edges match {
        case Some(ed) => ed.flatMap(eee => Set(s"${mapping(eee.fromCode)} <${eee.edgeName}> ${mapping(eee.toCode)} ."))
        case None => Set.empty
      })
  }


  def queryNodes(nodeKeys: Seq[NodeKey]) = {
    asyncQuery[FetchUidsResponse, FetchUidsResponse](clients.getClient().newReadOnlyTransaction(),
      DgraphQueries.nodesQuery(nodeKeys), Map(), s => s, (_) => true,
      (json) => {
        Try{
          println(json)
          JsonUtil.fromJson[FetchUidsResponse](json)
        } match {
          case Success(value) => value
          case Failure(exception) => {
            exception.printStackTrace()
            throw exception
          }
        }
      }
    )
  }

  def asyncQuery[T, R](transaction: AsyncTransaction,
                       query: String,
                       vars: Map[String, String],
                       transformFn: R => T,
                       successFn: R => Boolean,
                       jsonFn: String => R) = {
    transaction.setBestEffort(true)
    val response = transaction.query(query)

    val p = Promise[T]()
    // keep this to catch error
    response.get()
    response.thenAccept(c => {
      val nodes = jsonFn(c.getJson.toStringUtf8())
      successFn(nodes) match {
        case true => p.success(transformFn(nodes))
        case false => p.failure(new Exception(s"Node does not exist."))
      }})
    p.future
  }



}
