package com.example

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.io.IO
import spray.can.Http
import com.example.domain.Messages.{HttpConfig, GetHttpConfig}
import scala.util.{Failure, Success}


object Boot extends App {
  implicit val system = ActorSystem()

  // the handler actor replies to incoming HttpRequests
  val handler = system.actorOf(Props[CoffeePersistenceServiceActor], "coffee-service")

  import system.dispatcher
  implicit val timeout = Timeout(5.seconds)

  (handler ? GetHttpConfig).onComplete {
    case Success(HttpConfig(_, host, port)) =>
      IO(Http) ! Http.Bind(handler, host, port, 1000)
    case Success(s) =>
      throw new IllegalStateException(s"Unexpected result: $s")
    case Failure(t) =>
      t.printStackTrace()
  }
}