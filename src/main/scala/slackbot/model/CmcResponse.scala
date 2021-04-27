package slackbot.model

import io.circe._
import io.circe.generic.semiauto._

sealed trait CmcResponse

case class IdMapResponse(data: List[IdMap], status: Status)        extends CmcResponse
case class QuoteResponse(data: Map[String, Quote], status: Status) extends CmcResponse
case class ErrorResponse(status: Status)                           extends CmcResponse

object CmcResponse {
  implicit def decoderIdMap: Decoder[IdMapResponse] = deriveDecoder
  implicit def decoderQuote: Decoder[QuoteResponse] = deriveDecoder
  implicit def decoderError: Decoder[ErrorResponse] = deriveDecoder
}
