package slackbot.model

import zio._
import zio.logging._
import zio.clock._
import sttp.model.Uri
import sttp.client3.basicRequest
import sttp.client3.httpclient.zio._

import zio.duration._
import slackbot.client.CoinMarketCapClient

sealed trait SlackCommand {
  def name: String
  def args: String
  def responseUrl: Uri
  def process: RIO[Logging with SttpClient with Clock with Has[CoinMarketCapClient], Unit]
}

case class CheckCommand(args: String, responseUrl: Uri) extends SlackCommand {
  override def name: String = "check"
  override def process: RIO[Logging with SttpClient with Clock with Has[CoinMarketCapClient], Unit] =
    for {
      _ <- ZIO.sleep(2.seconds)
      _ <- log.debug(s"Processing $name command - responseUrl $responseUrl")
      r <- CoinMarketCapClient.getCryptoMap
      _ <- log.debug(s"${r.status}")
      symbol = args
      _ <- ZIO.whenCase(r.data.find(_.symbol == symbol).map(_.id)) {
        case Some(id) =>
          log.debug(s"$symbol - $id") *> CoinMarketCapClient
            .getCryptoQuote(id)
            .flatMap(q =>
              sendResponse(
                s"""{"text": "${q.data.values.head}"}"""
              )
            )
        case None =>
          log.error(s"$symbol not found") *> sendResponse(
            s"""{"text": "$symbol NOT FOUND"}"""
          )
      }
    } yield ()

  private def sendResponse(body: String): RIO[Logging with SttpClient, Unit] =
    send(
      basicRequest
        .post(responseUrl)
        .body(body)
        .contentType("application/json")
    ).unit
}

case class TestCommand(args: String, responseUrl: Uri) extends SlackCommand {
  override val name: String = "test"
  override def process: RIO[Logging, Unit] =
    log.debug(s"Processing $name command - responseUrl $responseUrl")
}
