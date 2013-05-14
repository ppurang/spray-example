package spray.httpx

import net.liftweb.json.Serialization._
import net.liftweb.json._
import spray.httpx.marshalling.Marshaller
import spray.httpx.unmarshalling.Unmarshaller
import spray.http._
import MediaTypes._


/**
 * A trait providing automatic to and from JSON marshalling/unmarshalling for case classes via lift-json.
 * Note that *spray-routing* does not have an automatic dependency on *lift-json*. You'll need to provide the
 * appropriate *lift-json* artifacts yourself.
 */
trait LiftJsonSupport {

  /**
   * The `Formats` to use for (de)serialization.
   */
  implicit def liftJsonFormats: Formats

  implicit def liftJsonUnmarshaller[T :Manifest] =
    Unmarshaller[T](`application/json`) {
      case x: HttpBody =>
        val jsonSource = x.asString
        parse(jsonSource).extract[T]
    }

  implicit def liftJsonMarshaller[T <: AnyRef] =
    Marshaller.delegate[T, String](`application/json`)(write(_))

}