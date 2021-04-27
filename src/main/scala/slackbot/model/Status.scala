package slackbot.model

import io.circe._, io.circe.generic.semiauto._

final case class Status(
  timestamp: String,
  error_code: Int,
  error_message: Option[String],
  elapsed: Int,
  credit_count: Int
)

object Status {
  implicit val decoder: Decoder[Status] = deriveDecoder[Status]
}
