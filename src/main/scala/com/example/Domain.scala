package com.example.domain

import play.api.libs.functional.syntax._
import play.api.libs.json._
import spray.http._
import spray.http.MediaTypes.CustomMediaType
import net.liftweb.json.{Serialization, DefaultFormats}
import spray.httpx.marshalling.Marshaller
import net.liftweb.json.Serialization._
import spray.httpx.unmarshalling.Unmarshaller
import net.liftweb.json._
import spray.util._

sealed trait Status
object Pending extends Status {
  override def toString = "Pending"
}
object Paid extends Status {
  override def toString = "Paid"
}
object Preparing extends Status {
  override def toString = "Preparing"
}
object Served extends Status {
  override def toString = "Served"
}

case class Next(rel: String, uri: String, `type`: String)

//todo Order should ideally be better typed          and that status should be something other than a string
case class Order(drink: String, cost: Option[Double] = None, next: Option[Next] = None, status : Option[String] = Option("pending"))

case class Payment(creditCardNo: String, expires: String, name: String, amount: Double)

object Order {

  implicit val format = DefaultFormats
  //implicit val formats = Serialization.formats(NoTypeHints)

  val `application/vnd.coffee+json`: CustomMediaType = CustomMediaType("application/vnd.coffee+json")
  MediaTypes.register(`application/vnd.coffee+json`)

/*
  val `application/vnd.payment+json`: CustomMediaType = CustomMediaType("application/vnd.payment+json")
  MediaTypes.register(`application/vnd.payment+json`)
*/



  implicit val OrderMarshaller = Marshaller.of[Order](`application/vnd.coffee+json`) {
    (value, contentType, ctx) =>
      ctx.marshalTo(HttpBody(contentType, write(value)))
  }

  implicit val OrderUnMarshaller = Unmarshaller[Order](`application/vnd.coffee+json`) {
    case HttpBody(contentType, buffer) =>
      // unmarshal from the string format used in the marshaller example
      parse(buffer.asString).extract[Order]

    // if we had meaningful semantics for the EmptyEntity
    // we could add a case for the EmptyEntity:
    // case EmptyEntity => ...
  }

  implicit val PaymentMarshaller = Marshaller.of[Payment](`application/vnd.coffee+json`) {
    (value, contentType, ctx) =>
      ctx.marshalTo(HttpBody(contentType, write(value)))
  }

  implicit val PaymentUnMarshaller = Unmarshaller[Payment](`application/vnd.coffee+json`) {
    case HttpBody(contentType, buffer) =>
      // unmarshal from the string format used in the marshaller example
      parse(buffer.asString).extract[Payment]

    // if we had meaningful semantics for the EmptyEntity
    // we could add a case for the EmptyEntity:
    // case EmptyEntity => ...
  }


  implicit val nextFmt = Json.format[Next]
  implicit val orderFmt = Json.format[Order]
}

