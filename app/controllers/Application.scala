package controllers

import models.Gist
import org.joda.time.DateTime
import play.api.libs.EventSource
import play.api.libs.iteratee.{Enumeratee, Enumerator}
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.libs.ws.WS
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// A Simple controller for Captaindash test
object Application extends Controller {

  // Our main page
  def index = Action {
    implicit request =>
      Ok(views.html.index())
  }

  // A Server-sent Event endpoint
  def loadLastGists = Action {
    implicit request =>
      import scala.concurrent.duration._
      import scala.language.postfixOps

      // Use this url for prod
      val urlProd = "https://api.github.com/gists/public"
      // Or this one, if your github quota is dead... he he he
      val urlTest = "http://localhost:9000/fakedata"

      def futureJsonResult: Future[Option[Seq[Gist]]] = WS.url(urlTest).get().map {
        response =>
          response.status match {
            case 200 =>
              Json.parse(response.body).asOpt[Seq[Gist]] // TODO améliorer la validation ici
            case 403 =>
              play.Logger.error("Quota exceed, use an authenticated call or FakeGithub endpoint.")
              None
            case other =>
              play.Logger.error(s"Something wrong happened $other")
              None
          }
      }

      val lastGists: Enumerator[Seq[Gist]] = Enumerator.generateM[Seq[Gist]] {
        futureJsonResult.flatMap(r => play.api.libs.concurrent.Promise.timeout(r, 2 seconds))
      }

      val asJSON: Enumeratee[Seq[Gist], JsValue] = Enumeratee.map {
        gists =>
         // TODO transformer la Seq[Gist] en un JsValue, ce JsValue doit etre un array json, et chaque élément un json (le gist repassé en JSON)
          JsString("Je ne suis pas implémenté mais je peux vous donner l'heure à la place. Il est => "+new DateTime().toString("HH:mm:ss"))
      }

      // TODO classer par ordre chronologique les Gists, du plus récent au plus vieux. A faire où vous voulez, pas forcément ici

      Ok.feed(lastGists &> asJSON &> EventSource()).as("text/event-stream")
  }
}