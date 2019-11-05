package org.dgraph.db

import io.dgraph.DgraphAsyncClient

import scala.util.Random


object DBConnectionHolder {
  def apply(clients: Seq[DgraphAsyncClient]): DBConnectionHolder = new DBConnectionHolder(clients)
}

class DBConnectionHolder(clients: Seq[DgraphAsyncClient]) {

  val random = new Random

//  val c = clients(0)

  def getClient() = clients(random.nextInt(clients.length))
}
