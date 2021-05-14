package slackbot

import zio._
import zio.logging._
import zio.config._
import zio.clock._
import zio.console._
import zio.magic._
import slackbot.config.Config
import slackbot.client.CoinMarketCapClient
import sttp.client3.httpclient.zio._
import sttp.model.Uri
import zio.crypto.mac._
import zhttp.http._
import zhttp.service._

import java.net.URLDecoder

import slackbot.utils.SlackUtils.validated
import slackbot.service.CommandProcessor
import slackbot.model._

object CryptoSlackBot extends App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    program
      .injectCustom(
        logEnv,
        Config.live,
        CoinMarketCapClient.live,
        HttpClientZioBackend.layer(),
        Clock.live,
        CommandProcessor.live,
        HMAC.live
      )
      .exitCode

  val logEnv =
    Logging.console(
      logLevel = LogLevel.Debug,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("ZIO demo")

  private def slack: HttpApp[Console with Logging with Has[CoinMarketCapClient] with SttpClient with Clock with Has[
    CommandProcessor
  ], Throwable] =
    HttpApp.collectM {
      case req @ Method.POST -> Root / "slack" / "echo" =>
        handleRequest(req)

      case req @ Method.POST -> Root / "slack" / "check" =>
        for {
          _ <- log.info(s"received request on ${req.endpoint}")
          form = extractFormData(req)
          _ <- log.info(form.toString())
          _ <- CommandProcessor.enqueueCommand(
            CheckCommand(
              args = form("text"),
              responseUrl = Uri.parse(form("response_url")).getOrElse(Uri("example.com"))
            )
          )
        } yield Response.ok
    }

  private def extractFormData(r: Request): Map[String, String] =
    r.getBodyAsString
      .map(URLDecoder.decode(_, "UTF-8"))
      .map(
        _.split("&").foldLeft(Map[String, String]())((m, kv) =>
          m + (kv.split("=").toList match {
            case k :: v :: Nil => (k -> v)
            case k :: Nil      => (k -> "")
            case _             => throw new RuntimeException("ERROR")
          })
        )
      )
      .getOrElse(Map[String, String]())

  private def handleRequest(req: Request): URIO[Logging with Has[CommandProcessor], UResponse] =
    for {
      form <- ZIO.succeed(extractFormData(req))
      _ <- CommandProcessor.enqueueCommand(
        EchoCommand(
          args = form("text"),
          responseUrl = Uri.parse(form("response_url")).getOrElse(Uri("example.com"))
        )
      )
    } yield Response.ok

  def test: UHttpApp = Http.collect { case Method.GET -> Root / "test" => Response.text("Hello from CryptoBot!") }

  def app(secret: String) = test +++ validated(secret)(HttpApp.forbidden("Not allowed!"), slack)

  private val program: RIO[Logging with Has[Config] with Has[
    CoinMarketCapClient
  ] with SttpClient with Clock with Console with Has[CommandProcessor] with HMAC.HMAC, Unit] =
    (for {
      _ <- log.info("Hello from ZIO CryptoBot!")
      c <- getConfig[Config]
      _ <- log.info(s"httpPort: ${c.port} cryptoRefreshPeriod: ${c.cryptoRefreshPeriod}")
      _ <- CoinMarketCapClient.initialize(c.cryptoRefreshPeriod).forkDaemon
      _ <- CommandProcessor.processCommands.forkDaemon
      _ <- Server.start(c.port, app(c.slackSigningSecret)).unit
    } yield ())

}
