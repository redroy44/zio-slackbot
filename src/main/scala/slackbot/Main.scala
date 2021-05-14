package slackbot

import zio._
import zio.logging._
import zio.config._
import zio.clock._
import zio.magic._
import slackbot.config.Config
import slackbot.client.CoinMarketCapClient
import sttp.client3.httpclient.zio._
import zhttp.http._
import zhttp.service._

import slackbot.utils.SlackUtils.validated
import slackbot.service.CommandProcessor
import slackbot.routes.Routes

object CryptoSlackBot extends App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    program
      .injectCustom(
        logEnv,
        Config.live,
        CoinMarketCapClient.live,
        HttpClientZioBackend.layer(),
        Clock.live,
        CommandProcessor.live
      )
      .exitCode

  val logEnv =
    Logging.console(
      logLevel = LogLevel.Debug,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("ZIO demo")

  def test: UHttpApp = Http.collect { case Method.GET -> Root => Response.text("Hello from CryptoBot!") }

  def app(secret: String) = test +++ validated(secret)(HttpApp.forbidden("Not allowed!"), Routes.slack)

  private val program: RIO[Logging with Has[Config] with Has[CoinMarketCapClient] with SttpClient with Clock with Has[
    CommandProcessor
  ], Unit] =
    (for {
      _ <- log.info("Hello from ZIO CryptoBot!")
      c <- getConfig[Config]
      _ <- log.info(s"httpPort: ${c.port} cryptoRefreshPeriod: ${c.cryptoRefreshPeriod}")
      _ <- CoinMarketCapClient.initialize(c.cryptoRefreshPeriod).forkDaemon
      _ <- CommandProcessor.processCommands.forkDaemon
      _ <- Server.start(c.port, app(c.slackSigningSecret)).unit
    } yield ())

}
