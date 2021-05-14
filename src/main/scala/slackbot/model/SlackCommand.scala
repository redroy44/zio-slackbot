package slackbot.model

import zio._
import zio.logging._
import sttp.model.Uri
import sttp.client3.basicRequest
import sttp.client3.httpclient.zio._

import slackbot.client.CoinMarketCapClient

sealed trait SlackCommand {
  def name: String
  def args: String
  def responseUrl: Uri
  def process: RIO[Logging with SttpClient with Has[CoinMarketCapClient], Unit]

  protected def sendResponse(body: String): RIO[SttpClient, Unit] =
    send(
      basicRequest
        .post(responseUrl)
        .body(s"""{"text": "$body"}""")
        .contentType("application/json")
    ).unit
}

case class CheckCommand(args: String, responseUrl: Uri) extends SlackCommand {
  override def name: String = "check"
  override def process: RIO[Logging with SttpClient with Has[CoinMarketCapClient], Unit] =
    for {
      _         <- log.debug(s"Processing $name command - args: $args")
      cryptoMap <- CoinMarketCapClient.cryptoMap
      symbol = args.toUpperCase
      _ <- ZIO.whenCase(cryptoMap.get(symbol).map(_.id)) {
        case Some(id) =>
          log.debug(s"$symbol - $id") *> CoinMarketCapClient
            .getCryptoQuote(id)
            .flatMap(q => sendResponse(q.data.values.head.toString))
        case None =>
          log.error(s"$symbol not found") *> sendResponse(
            s"$symbol NOT FOUND"
          )
      }
    } yield ()
}

case class EchoCommand(args: String, responseUrl: Uri) extends SlackCommand {
  override val name: String = "echo"
  override def process: RIO[Logging with SttpClient, Unit] =
    for {
      _ <- log.debug(s"Processing $name command - args: $args")
      _ <- sendResponse(s"$name command - $args")
    } yield ()
}