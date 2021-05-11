package slackbot.client

import zio._
import zio.config._
import zio.logging._
import zio.stream._
import zio.clock._
import zio.duration._
import sttp.client3._
import sttp.client3.circe._
import sttp.client3.httpclient.zio._
import sttp.model._

import slackbot.model._
import slackbot.model.CmcResponse._
import slackbot.config.Config

trait CoinMarketCapClient {
  def getCryptoMap: RIO[SttpClient with Logging, IdMapResponse]

  def getCryptoQuote(id: Int): RIO[SttpClient with Logging, QuoteResponse]

  def initialize(cryptoRefreshPeriod: Duration): RIO[SttpClient with Logging with Clock, Unit]

  def cryptoMap: Ref[Map[String, IdMap]]
}

object CoinMarketCapClient {
  val live: URLayer[SttpClient with Logging with Has[Config], Has[CoinMarketCapClient]] = CoinMarketCapClientLive.layer

  def getCryptoMap: RIO[Has[CoinMarketCapClient] with SttpClient with Logging, IdMapResponse] =
    ZIO.accessM[Has[CoinMarketCapClient] with SttpClient with Logging](_.get.getCryptoMap)

  def getCryptoQuote(id: Int): RIO[Has[CoinMarketCapClient] with SttpClient with Logging, QuoteResponse] =
    ZIO.accessM[Has[CoinMarketCapClient] with SttpClient with Logging](_.get.getCryptoQuote(id))

  def initialize(
    cryptoRefreshPeriod: Duration
  ): RIO[Has[CoinMarketCapClient] with SttpClient with Logging with Clock, Unit] =
    ZIO.accessM[Has[CoinMarketCapClient] with Logging with Clock with SttpClient](
      _.get.initialize(cryptoRefreshPeriod)
    )

  def cryptoMap: URIO[Has[CoinMarketCapClient], Map[String, IdMap]] =
    ZIO.accessM[Has[CoinMarketCapClient]](_.get.cryptoMap.get)

}

case class CoinMarketCapClientLive(apiKey: String, cmcEndpoint: String, cryptoMap: Ref[Map[String, IdMap]])
    extends CoinMarketCapClient {
  def getCryptoMap: RIO[SttpClient with Logging, IdMapResponse] =
    (for {
      uri <- ZIO.fromEither(
        Uri
          .parse(cmcEndpoint + "/v1/cryptocurrency/map")
          .left
          .map(_ => HttpError[String]("Unparseable github url.", StatusCode(400)))
      )
      request = basicRequest
        .acceptEncoding("application/json")
        .headers(createHeaders(apiKey))
        .get(uri)
        .response(asJsonEither[ErrorResponse, IdMapResponse])
      _   <- log.debug(request.toCurl)
      res <- send(request)
    } yield (res.body)).absolve

  def getCryptoQuote(id: Int): RIO[SttpClient with Logging, QuoteResponse] =
    (for {
      uri <- ZIO.fromEither(
        Uri
          .parse(cmcEndpoint + s"/v1/cryptocurrency/quotes/latest?id=$id")
          .left
          .map(_ => HttpError[String]("Unparseable github url.", StatusCode(400)))
      )
      request = basicRequest
        .acceptEncoding("application/json")
        .headers(createHeaders(apiKey))
        .get(uri)
        .response(asJsonEither[ErrorResponse, QuoteResponse])
      _   <- log.debug(request.toCurl)
      res <- send(request)
    } yield (res.body)).absolve

  def initialize(cryptoRefreshPeriod: Duration): RIO[SttpClient with Logging with Clock, Unit] =
    Stream
      .fromSchedule(Schedule.once andThen Schedule.fixed(cryptoRefreshPeriod))
      .tap { i =>
        for {
          _           <- log.debug(s"Updating CryptoMap - tick: $i")
          mapResponse <- getCryptoMap
          _ <- cryptoMap.set(
            mapResponse.data.foldLeft(Map[String, IdMap]())((map, elem) => map + (elem.symbol -> elem))
          )
          _ <- log.debug(s"CryptoMap updated - tick: $i")
        } yield ()
      }
      .runDrain

  private def createHeaders(apiKey: String): Map[String, String] =
    Map("X-CMC_PRO_API_KEY" -> s"$apiKey")
}

object CoinMarketCapClientLive {
  val layer: ZLayer[SttpClient with Logging with Has[Config], Nothing, Has[CoinMarketCapClient]] = {
    for {
      conf      <- getConfig[Config]
      endpoint  <- ZIO.succeed("https://pro-api.coinmarketcap.com")
      cryptoMap <- Ref.make(Map[String, IdMap]())
    } yield CoinMarketCapClientLive(conf.cmcApiKey, endpoint, cryptoMap)
  }.toLayer
}
