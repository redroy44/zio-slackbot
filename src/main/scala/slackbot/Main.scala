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

import zhttp.http._
import zhttp.service._

import java.net.URLDecoder

import slackbot.utils.SlackUtils
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
        CommandProcessor.live
      )
      .exitCode

  val logEnv =
    Logging.console(
      logLevel = LogLevel.Debug,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("ZIO demo")

  private def app: Http[Console with Logging with Has[CoinMarketCapClient] with SttpClient with Clock with Has[
    CommandProcessor
  ], Throwable] =
    Http.collectM {
      case req @ Method.POST -> Root / "slack" / "echo" =>
        SlackUtils.validateRequest(req)(handleRequest)

      case req @ Method.POST -> Root / "slack" / "check" =>
        SlackUtils.validateRequest(req) { r =>
          for {
            _ <- log.info(s"received request on ${r.endpoint}")
            form = extractFormData(r)
            _ <- log.info(form.toString())
            _ <- CommandProcessor.enqueueCommand(
              CheckCommand(
                args = form("text"),
                responseUrl = Uri.parse(form("response_url")).getOrElse(Uri("example.com"))
              )
            )
          } yield Response.ok
        }
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

  private val program: RIO[Logging with Has[Config] with Has[
    CoinMarketCapClient
  ] with SttpClient with Clock with Console with Has[CommandProcessor], Unit] =
    (for {
      _ <- log.info("Hello from ZIO slackbot")
      c <- getConfig[Config]
      _ <- log.info(s"httpPort: ${c.port} cryptoRefreshPeriod: ${c.cryptoRefreshPeriod}")
      _ <- CoinMarketCapClient.initialize(c.cryptoRefreshPeriod).fork
      _ <- CommandProcessor.processCommands.fork
      _ <- Server.start(c.port, app).unit
    } yield ())

}
