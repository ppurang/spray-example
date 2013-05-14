package com.example

import org.specs2.mutable.Specification
import com.example.domain._
import com.example.domain.Order._
import play.api.libs.json.Json
import net.liftweb.json._


object DomainSpec extends Specification {
  "Domian" should {
    "allow desrializing an order" in {
      println(Json.toJson(Order("latte")))
      println("""{"drink":"latte"}""")
      parse("""{"drink":"latte"}""").extract[Order] === Order("latte")
      //Json.toJson("""{"drink":"latte"}""").as[Order] === Order("latte")
      Json.toJson(Json.toJson(Order("latte"))).as[Order] === Order("latte")     //WTH? //todo WARUM?
    }
  }
}