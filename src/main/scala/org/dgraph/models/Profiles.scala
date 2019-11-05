package org.dgraph.models

case class Details(yob: Int, gender: String)
case class Profiles(profile: Seq[String], details: Details)
