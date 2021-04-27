package slackbot.model

import io.circe._, io.circe.generic.semiauto._

sealed trait Data

case class IdMap(
  id: Int,
  name: String,
  symbol: String,
  slug: String,
  is_active: Int,
  status: Option[String],
  first_historical_data: String,
  last_historical_data: String
) extends Data

object IdMap {
  implicit val decoder: Decoder[IdMap] = deriveDecoder[IdMap]
}

case class Quote(
  id: Int,
  name: String,
  symbol: String,
  slug: String,
  is_fiat: Int,
  cmc_rank: Int,
  num_market_pairs: Int,
  circulating_supply: BigDecimal,
  total_supply: BigDecimal,
  market_cap_by_total_supply: Option[BigDecimal],
  max_supply: Option[BigDecimal],
  date_added: String,
  tags: List[String],
  last_updated: String,
  quote: Map[String, QuoteInternal]
) extends Data

object Quote {
  implicit val decoder: Decoder[Quote] = deriveDecoder[Quote]
}

case class QuoteInternal(
  price: BigDecimal,
  volume_24h: BigDecimal,
  volume_24h_reported: Option[BigDecimal],
  volume_7d: Option[BigDecimal],
  volume_7d_reported: Option[BigDecimal],
  volume_30d: Option[BigDecimal],
  volume_30d_reported: Option[BigDecimal],
  market_cap: BigDecimal,
  percent_change_1h: BigDecimal,
  percent_change_24h: BigDecimal,
  percent_change_7d: BigDecimal,
  percent_change_30d: BigDecimal,
  last_updated: String
)

object QuoteInternal {
  implicit val decoder: Decoder[QuoteInternal] = deriveDecoder[QuoteInternal]
}
