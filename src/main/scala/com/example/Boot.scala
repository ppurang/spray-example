package com.example

import akka.actor._
import akka.io.IO
import spray.can.Http


object Boot extends App {

  implicit val system = ActorSystem()

  // the handler actor replies to incoming HttpRequests
  val handler = system.actorOf(Props[CoffeeServiceActor], "coffee-service")


  IO(Http) ! Http.Bind(handler, "localhost", 8080, 1000)
}