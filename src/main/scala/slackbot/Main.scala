package slackbot

import zio._
import zio.logging._
import zio.config._
import zio.clock._
import zio.console._
import zio.magic._
import slackbot.config.Config
import slackbot.client.CoinMarketCapClient
// import sttp.client3.basicRequest
import sttp.client3.httpclient.zio._
import sttp.model.Uri

import zhttp.http._
import zhttp.service._

// import zio.duration._

import java.net.URLDecoder

import slackbot.utils.SlackUtils
import slackbot.service.CommandProcessor
import slackbot.model.CheckCommand

object WebSocketZio extends App {

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
      case req @ Method.POST -> Root / "slack" / "subscribe" =>
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

      case req @ Method.POST -> Root / "slack" / "check2" =>
        //SlackUtils.validateRequest(req)(handle)
        SlackUtils
          .validateRequest(req) { r =>
            (for {
              _   <- log.info(s"received request on ${r.endpoint}")
              rrr <- CoinMarketCapClient.getCryptoQuote(4642)
              _   <- log.debug(s"${rrr.data.toString()}")
            } yield Response.jsonString(
              //s"test ${r.getBodyAsString.get} ${rrr.data}"
              s"""
                 | {
                 | "blocks": [
                 | {
                 | "type": "section",
                 | "text": {
                 | "type": "mrkdwn",
                 | "text": "test ${r.getBodyAsString.get} ${rrr.data}"
                 | },
                 | "accessory": {
                 | "type": "image",
                 | "image_url": "https://pbs.twimg.com/profile_images/625633822235693056/lNGUneLX_400x400.jpg",
                 | "alt_text": "cute cat"
                 | }
                 | }
                 | ]
                 | }
              """.stripMargin
            )).catchAll(_ => UIO(HttpError.InternalServerError("ERROR").toResponse))
          }
    }

  private def extractFormData(r: Request): Map[String, String] =
    r.getBodyAsString
      .map(URLDecoder.decode(_, "UTF-8"))
      .map(
        _.split("&").foldLeft(Map[String, String]())((m, kv) => m + (kv.split("=")(0) -> kv.split("=")(1)))
      )
      .getOrElse(Map[String, String]())

  // private def handle(r: Request): ZIO[Logging with Has[CoinMarketCapClient] with SttpClient, Nothing, UResponse] =
  //   (for {
  //     _   <- log.info(s"received request on ${r.endpoint}")
  //     rrr <- CoinMarketCapClient.getCryptoMap
  //   } yield Response.text(s"test ${r.getBodyAsString.get} ${rrr.data.toString}")).catchAll(_ =>
  //     UIO(HttpError.InternalServerError("ERROR").toResponse)
  //   )

  private def handleRequest(req: Request): UIO[UResponse] = {
    val form = req.getBodyAsString
      .map(
        _.split("&").foldLeft(Map[String, String]())((m, kv) => m + (kv.split("=")(0) -> kv.split("=")(1)))
      )
      .getOrElse(Map[String, String]())

    UIO(Response.text(s"Form data: ${form("user_name")} - ${form("text")}"))
  }

  private val program: RIO[Logging with Has[Config] with Has[
    CoinMarketCapClient
  ] with SttpClient with Clock with Console with Has[CommandProcessor], Unit] =
    (for {
      _ <- log.info("Hello from ZIO logger")
      c <- getConfig[Config]
      _ <- log.info(s" apiKey: ${c.cmcApiKey}")
      // r <- CoinMarketCapClient.getCryptoMap
      // _ <- log.info(s"${r.status}")
      // symbol = "CHSB"
      // id <- ZIO
      //   .fromOption(r.data.find(_.symbol == symbol).map(_.id))
      //   .mapError(_ => new RuntimeException("Specified crypto not found!"))
      // q <- CoinMarketCapClient
      //   .getCryptoQuote(id)
      // _ <- log.info(s"${q.data.values.head.quote("USD")}")
      _ <- CommandProcessor.processCommands.fork
    } yield ()) *> Server.start(8090, app)

}
