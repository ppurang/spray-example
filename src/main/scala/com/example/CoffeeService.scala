package com.example

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import spray.http.HttpHeaders.Location
import spray.http.StatusCodes._
import play.api.libs.json.Json
import com.example.domain._
import com.example.domain.Order._
import spray.httpx.LiftJsonSupport
import net.liftweb.json.Formats
import shapeless.HNil

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class CoffeeServiceActor extends Actor with CoffeeService with HashMapPersistenceCoffee with VeryPriceyCoffee {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(orderRoute)
}

trait HashMapPersistenceCoffee extends PersistenceCoffee {
  private var map = collection.mutable.Map[Int, Order]()
  private var counter = 0

  def persist(order: Order) = {
    counter = counter + 1
    val porder = order.copy(next = Option(Next("payment", s"https://localhost:8080/payment/order/$counter", "application/vnd.coffee+json")))
    map += (counter -> porder)
    (counter, porder)
  }


  def update(counter: Int, porder: Order) {
    map -= counter
    map += (counter -> porder)
  }

  def retrieve(id: Int) = map(id)
}

trait PersistenceCoffee {
  def persist(order: Order): (Int, Order)

  def update(counter: Int, porder: Order): Unit

  def retrieve(id: Int): Order
}

trait PriceCoffee {
  def price(order: Order): Order
}

trait VeryPriceyCoffee extends PriceCoffee {
  def price(order: Order): Order = order.copy(cost = Some(100.0))
}

// this trait defines our service behavior independently from the service actor
trait CoffeeService extends HttpService with LiftJsonSupport {
  self: PersistenceCoffee with PriceCoffee =>

  override implicit def liftJsonFormats: Formats = Order.format


  val orderRoute =
    pathPrefix("order") {
      respondWithMediaType(`application/vnd.coffee+json`) {
        path("") {
          post {
            entity(as[Order]) {
              order => complete {
                val (counter, persistedOrder) = persist(price(order))
                (Created, Location(s"http://localhost:8080/order/$counter") :: Nil, persistedOrder)
              }
            }
          }
        } ~ path(IntNumber) {
          id =>
            get {
              complete {
                retrieve(id)
              }
            }
        }
      }
    }
}

trait CoffeePaymentService extends HttpService with LiftJsonSupport {
  self: PersistenceCoffee =>

  override implicit def liftJsonFormats: Formats = Order.format

  val paymentRoute =
    pathPrefix("payment/order") {
      respondWithMediaType(`application/vnd.payment+json`) {
        path(IntNumber) {
          id =>
            get {
              complete {
                retrieve(id)
              }
            } ~ put {
              entity(as[Payment]) {
                payment => complete {
                  val porder = retrieve(id)
                  if(porder.status != "paid" && Option(payment.amount) == porder.cost) { //todo really?
                    update(id, porder.copy(status = "paid"))
                    (OK, porder)
                  }  else {
                    (Conflict, s"order.status: $porder.status, order.cost: $porder.status, payment.amount: $payment.amount")
                  }
                }
              }
            }

        }
      }
    }
}