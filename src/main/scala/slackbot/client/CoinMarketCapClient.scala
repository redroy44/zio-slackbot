package slackbot.client

import zio._
import zio.config._
import zio.logging._
import sttp.client3._
import sttp.client3.circe._
import sttp.client3.httpclient.zio._
import sttp.model._

//import io.circe.generic.auto._

import slackbot.model._
import slackbot.model.CmcResponse._
import slackbot.config.Config

trait CoinMarketCapClient {
  def getCryptoMap: RIO[SttpClient with Logging, IdMapResponse]

  def getCryptoQuote(id: Int): RIO[SttpClient with Logging, QuoteResponse]

}

object CoinMarketCapClient {
  val live: URLayer[SttpClient with Logging with Has[Config], Has[CoinMarketCapClient]] = CoinMarketCapClientLive.layer

  def getCryptoMap: RIO[Has[CoinMarketCapClient] with SttpClient with Logging, IdMapResponse] =
    ZIO.accessM[Has[CoinMarketCapClient] with SttpClient with Logging](_.get.getCryptoMap)

  def getCryptoQuote(id: Int): RIO[Has[CoinMarketCapClient] with SttpClient with Logging, QuoteResponse] =
    ZIO.accessM[Has[CoinMarketCapClient] with SttpClient with Logging](_.get.getCryptoQuote(id))
}

case class CoinMarketCapClientLive(apiKey: String, cmcEndpoint: String) extends CoinMarketCapClient {
  def getCryptoMap: RIO[SttpClient with Logging, IdMapResponse] =
    (for {
      uri <- ZIO.fromEither(
        Uri
          .parse(cmcEndpoint + "/v1/cryptocurrency/map")
          .left
          .map(_ => HttpError[String]("Unparseable github url.", StatusCode(400)))
      )
      _ <- log.debug(s"$uri")
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
      _ <- log.debug(s"$uri")
      request = basicRequest
        .acceptEncoding("application/json")
        .headers(createHeaders(apiKey))
        .get(uri)
        .response(asJsonEither[ErrorResponse, QuoteResponse])
      _   <- log.debug(request.toCurl)
      res <- send(request)
    } yield (res.body)).absolve

  // def processResponse(
  //   response: Either[ResponseException[ResponseError, Error], CmcResponse]
  // ): RIO[SttpClient with Logging, IdMapResponse] =
  //   response.map {
  //     case Right(value) => Right(IdMapResponse(value.data, value.status))
  //     case Left(e) =>
  //       e match {
  //         case HttpError(body, statusCode) => Left(HttpError[String](body.toString, statusCode))
  //         case DeserializationException(body, error) =>
  //           Left(
  //             HttpError[String](s"Json $body failed with message ${error.getMessage}", StatusCode.InternalServerError)
  //           )
  //       }
  //   }.absolve

  private def createHeaders(apiKey: String): Map[String, String] =
    Map("X-CMC_PRO_API_KEY" -> s"$apiKey")

}

object CoinMarketCapClientLive {
  val layer: ZLayer[SttpClient with Logging with Has[Config], Nothing, Has[CoinMarketCapClient]] = {
    for {
      conf     <- getConfig[Config]
      endpoint <- ZIO.succeed("https://pro-api.coinmarketcap.com")
    } yield CoinMarketCapClientLive(conf.cmcApiKey, endpoint)
  }.toLayer
}
