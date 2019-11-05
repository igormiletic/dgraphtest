package org.dgraph.db


import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.HttpCookiePair
import com.fasterxml.jackson.core.JsonGenerator.Feature
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper


/**
 * Convert JSON object to Scala object
 * Convert Scala object class to JSON object
 */
object JsonUtil {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.configure(Feature.ESCAPE_NON_ASCII, true)

  def toJson(value: Map[Symbol, Any]): String = {
    toJson(value map { case (k,v) => k.name -> v})
  }

  def toJson(value: Any): String = {
    mapper.writeValueAsString(value)
  }

  def toMap[V](json:String)(implicit m: Manifest[V]) = fromJson[Map[String,V]](json)

  def fromJson[T](json: String)(implicit m : Manifest[T]): T = {
    mapper.readValue[T](json)
  }

  def headersToMap[T](f: String => T, obj: Seq[HttpHeader]) : Map[String, T] = obj.map(s => s.name -> f(s.value)).toMap

  def cookiesToMap[T](f: String => T, obj: Seq[HttpCookiePair]) : Map[String, T] = obj.map(s => s.name -> f(s.value)).toMap

}
