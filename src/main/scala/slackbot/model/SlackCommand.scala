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
}

case class CheckCommand(args: String, responseUrl: Uri) extends SlackCommand {
  override val name: String = "check"
}

case class EchoCommand(args: String, responseUrl: Uri) extends SlackCommand {
  override val name: String = "echo"
}

object SlackCommand {
  def process(c: SlackCommand): RIO[Logging with SttpClient with Has[CoinMarketCapClient], Unit] = c match {
    case CheckCommand(args, responseUrl) =>
      for {
        _         <- log.debug(s"Processing ${c.name} command - args: $args")
        cryptoMap <- CoinMarketCapClient.cryptoMap
        symbol = args.toUpperCase
        _ <- ZIO.whenCase(cryptoMap.get(symbol).map(_.id)) {
          case Some(id) =>
            log.debug(s"$symbol - $id") *> CoinMarketCapClient
              .getCryptoQuote(id)
              .flatMap(q => sendResponse(q.data.values.head.toString, responseUrl))
          case None =>
            log.error(s"$symbol not found") *> sendResponse(s"$symbol NOT FOUND", responseUrl)
        }
      } yield ()
    case EchoCommand(args, responseUrl) =>
      for {
        _ <- log.debug(s"Processing ${c.name} command - args: $args")
        _ <- sendResponse(s"${c.name} command - $args", responseUrl)
      } yield ()
  }

  private def sendResponse(body: String, url: Uri): RIO[SttpClient, Unit] =
    send(
      basicRequest
        .post(url)
        .body(s"""{"text": "$body"}""")
        .contentType("application/json")
    ).unit
}
