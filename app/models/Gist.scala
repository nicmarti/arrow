package models

import play.api.libs.json.Json

/**
 * A simple Gist
 */
// TODO retrouver le langage utilisé
case class Gist(id: String, url: String, description: Option[String])

// TODO parser ?
object Gist {
  implicit val gistFormat = Json.format[Gist]
}