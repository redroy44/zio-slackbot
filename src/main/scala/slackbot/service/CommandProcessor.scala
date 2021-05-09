package slackbot.service

import zio._
import zio.logging._
import zio.stream._
import zio.clock._

import slackbot.model.SlackCommand
import sttp.client3.httpclient.zio.SttpClient
import slackbot.client.CoinMarketCapClient

trait CommandProcessor {
  def enqueueCommand(cmd: SlackCommand): UIO[Unit]

  def processCommands: RIO[Logging with SttpClient with Clock with Has[CoinMarketCapClient], Unit]
}

object CommandProcessor {
  val live: ZLayer[Logging with SttpClient, Nothing, Has[CommandProcessor]] = CommandProcessorLive.layer

  def enqueueCommand(cmd: SlackCommand): URIO[Has[CommandProcessor], Unit] =
    ZIO.accessM[Has[CommandProcessor]](_.get.enqueueCommand(cmd))

  def processCommands
    : RIO[Has[CommandProcessor] with Logging with SttpClient with Clock with Has[CoinMarketCapClient], Unit] =
    ZIO.accessM[Has[CommandProcessor] with Logging with SttpClient with Clock with Has[CoinMarketCapClient]](
      _.get.processCommands
    )

}

case class CommandProcessorLive(queue: Queue[SlackCommand]) extends CommandProcessor {
  def enqueueCommand(cmd: SlackCommand): UIO[Unit] = queue.offer(cmd).unit

  def processCommands: RIO[Logging with SttpClient with Clock with Has[CoinMarketCapClient], Unit] =
    Stream.fromQueue(queue).tap(c => c.process).runDrain
}

object CommandProcessorLive {
  val layer: ZLayer[Logging, Nothing, Has[CommandProcessor]] = {
    for {
      queue <- Queue.unbounded[SlackCommand].toManaged(q => q.shutdown)
    } yield CommandProcessorLive(queue)
  }.toLayer
}
