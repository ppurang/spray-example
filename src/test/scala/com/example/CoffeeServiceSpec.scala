package com.example

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import spray.http.HttpMethods._
import StatusCodes._
import domain._
import Order._

class CoffeeServiceSpec extends Specification with Specs2RouteTest with CoffeeService with CoffeePaymentService with VeryPriceyCoffee with HashMapPersistenceCoffee {

  sequential

  def actorRefFactory = system

  "As a customer" should {

    "I want to order a coffee so that Coffeebucks can prepare my drink" in {
      Post("/order", Option(Order("latte"))) ~> orderRoute ~> check {
        val order = entityAs[Order]
        order.cost === Option(100.0)
      }
    }

    "I want to see the status of the order" in {
      Post("/order", Option(Order("latte"))) ~> orderRoute ~> check {
        val order = entityAs[Order]
        order.cost === Option(100.0)
      }
      Get("/order/1") ~> orderRoute ~> check {
        val order = entityAs[Order]
        order.drink === "latte"
        order.status === Some("pending")
      }
    }

    import spray.httpx.marshalling._
    "I want to pay for the hot cuppa before it turns cold" in {
      Post("/order", Option(Order("latte"))) ~> orderRoute ~> check {
        val order = entityAs[Order]
        order.cost === Option(100.0)
      }

      Put("/payment/order/1", Option(Payment("xxxx-yyyy-zzzz-aaaa", "01/17", "Jane Doe", 100.0))) ~> paymentRoute ~> check {
        status === OK
        val order = entityAs[Order]
        order.drink === "latte"
        order.status === Option("paid")
      }



/*
     HttpRequest(
        PUT,
        "/payment/order/1",
        HttpEntity(`application/vnd.payment+json`, Payment("xxxx-yyyy-zzzz-aaaa", "01/17", "Jane Doe", 100.0))
      ) ~> paymentRoute ~> check {
        status === OK
        val order = entityAs[Order]
        order.drink === "latte"
        order.status === Option("paid")
      }
*/

    }
  }
}
