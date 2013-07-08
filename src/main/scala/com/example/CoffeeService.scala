package com.example

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import spray.http.HttpHeaders.Location
import spray.http.StatusCodes._
import com.example.domain._
import com.example.domain.Order._
import spray.httpx.LiftJsonSupport
import net.liftweb.json.Formats
import spray.routing.directives.{CacheSpecMagnet, CompletionMagnet, MethodDirectives}
import HttpMethods._
import spray.httpx.marshalling._
import Directives._
import directives.RespondWithDirectives._
import directives.RouteDirectives._
import MethodDirectives._
import directives.CachingDirectives._
import spray.routing.PathMatchers.IntNumber
import com.typesafe.config.{ConfigFactory, Config}
import com.example.domain.Messages.{HttpConfig, GetHttpConfig}

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class CoffeePersistenceServiceActor extends Actor
                                       with Entry
                                       with CoffeeService
                                       with CoffeePaymentService
                                       with SlickH2CoffeePersistence
                                       with VeryPriceyCoffee
                                       with ConfigLinkBuilder
                                       with DefultConfigProvider {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(entry ~ orderRoute ~ paymentRoute) orElse {
    case GetHttpConfig =>
      sender ! HttpConfig(
        config getString "http.server.protocol", config getString "http.server.host", config getInt "http.server.port")
  }
}

trait PriceCoffee {
  def price(order: Order): Order
}

trait VeryPriceyCoffee extends PriceCoffee {
  def price(order: Order): Order = order.copy(cost = Some(100.0))
}

object Directives {

  case class Allow(method: HttpMethod, methods: HttpMethod*) extends HttpHeader {
    def name = "Allow"

    def lowercaseName = "allow"

    def value = (method +: methods).mkString(",")

    def render[R <: Rendering](r: R): r.type =  r ~~ name ~~ ':' ~~ ' ' ~~ value
  }

  def allowed(method: HttpMethod, methods: HttpMethod*): Route =
    options {
      val header = Allow(method, methods: _*)
      respondWithSingletonHeader(header) {
        complete {
          "Methods allowed: " + header.value
        }
      }
    }

  def someOrNotFound[A: Marshaller](s: Option[A]): CompletionMagnet = {
    s.fold[CompletionMagnet](NotFound)(a => a)
  }
}

trait Entry extends HttpService {
  self: LinkBuilder =>

  val entry = path("") {
//    alwaysCache(CacheSpecMagnet()) {
    allowed(GET) ~
    get {
        complete {
          Link("order", createLink("order"), "*/*,plain/text,application/json")
        }
      }

//    }
  }
}

trait LinkBuilder {
  def createLink(parts: Any*): String
}

trait ConfigLinkBuilder extends LinkBuilder {
  self: ConfigProvider =>

  def createLink(parts: Any*) = {
    val protocol = config getString "http.server.protocol"
    val host = config getString "http.server.host"
    val port = config getString "http.server.port"

    protocol + "://" + host + ":" + port + "/" + (parts mkString "/")
  }
}

trait ConfigProvider {
  def config: Config
}

trait DefultConfigProvider extends ConfigProvider {
  lazy val config = ConfigFactory.load()
}

// this trait defines our service behavior independently from the service actor
trait CoffeeService extends HttpService with LiftJsonSupport {
  self: CoffeePersistence with PriceCoffee with LinkBuilder =>

  override implicit def liftJsonFormats: Formats = Order.format

  val orderRoute =
    pathPrefix("order") {
      path("") {
          allowed(POST) ~
          post {
            entity(as[Order]) {
              order => complete {
                val (counter, persistedOrder) = persist(price(order), createLink("payment", "order", _))
                (Created, Location(createLink("order", counter)) :: Nil, persistedOrder)
              }
            }
          }
      } ~ path(IntNumber) {
        id => allowed(GET) ~
/*
        //this doesn't work: "can't render nothing"
        rejectEmptyResponse{
          get {
            complete {
              retrieve(id)
            }
          }
        }
*/
          get {
            complete {
              someOrNotFound(retrieve(id))
            }
          }
      }
    }
}

trait CoffeePaymentService extends HttpService with LiftJsonSupport {
  self: CoffeePersistence =>

  override implicit def liftJsonFormats: Formats = Order.format

  val paymentRoute =
   pathPrefix("payment" / "order") {
     path(IntNumber) {
         id => allowed(GET, PUT) ~
           get {
             complete {
               someOrNotFound(retrieve(id))
             }
           } ~ put {
           entity(as[Payment]) {
             payment => complete {
               val oporder = retrieve(id)
               oporder.fold[CompletionMagnet](NotFound) {
                 porder =>
                   if (porder.status != Option("paid") && Option(payment.amount) == porder.cost) {
                     //todo really?
                     val n = porder.copy(status = Option("paid"), next = None)
                     update(id, n)
                     (OK, n)
                   } else {
                     (Conflict, s"order.status: ${porder.status}, order.cost: ${porder.cost}, payment.amount: ${payment.amount}")
                   }
               }
             }
           }
         }
       }
   }
}