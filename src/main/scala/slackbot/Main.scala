package slackbot

// import sttp.client3._
// import sttp.client3.asynchttpclient.zio._
// import sttp.capabilities.zio.ZioStreams
import zio._
import zio.logging._
import zio.config._
import zio.magic._
import slackbot.config.Config
import slackbot.client.CoinMarketCapClient
import sttp.client3.httpclient.zio._
// import zio.console._

// import zio.stream.Stream
// import sttp.ws.WebSocketFrame

object WebSocketZio extends App {

  // def useWebSocket: Stream[Throwable, WebSocketFrame] => Stream[Throwable, WebSocketFrame] = { input =>
  //   Stream(WebSocketFrame.text("1")) ++ input.flatMap {
  //     case WebSocketFrame.Text("10", _, _) =>
  //       println("Received 10 messages, sending close frame")
  //       Stream(WebSocketFrame.close)
  //     case WebSocketFrame.Text(n, _, _) =>
  //       println(s"Received $n messages, replying with $n+1")
  //       Stream(WebSocketFrame.text((n.toInt + 1).toString))
  //     case _ => Stream.empty
  //   }

  // }

  // // create a description of a program, which requires two dependencies in the environment:
  // // the SttpClient, and the Console
  // val sendAndPrint: RIO[Console with SttpClient, Response[Unit]] =
  //   sendR(basicRequest.get(uri"wss://echo.websocket.org").response(asWebSocketStreamAlways(ZioStreams)(useWebSocket)))

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    program.injectCustom(logEnv, Config.live, CoinMarketCapClient.live, HttpClientZioBackend.layer()).exitCode

  val logEnv =
    Logging.console(
      logLevel = LogLevel.Debug,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("ZIO demo")

  private val program: RIO[Logging with Has[Config] with Has[CoinMarketCapClient] with SttpClient, Unit] = for {
    _ <- log.info("Hello from ZIO logger")
    c <- getConfig[Config]
    _ <- log.info(s" apiKey: ${c.cmcApiKey}")
    r <- CoinMarketCapClient.getCryptoMap //.catchAll(e => log.error(e.getMessage()))
    _ <- log.info(s"${r.status}")
    symbol = "BTC"
    _ <- log.info(s"${r.data.find(_.symbol == symbol)}")
    q <- CoinMarketCapClient.getCryptoQuote(r.data.find(_.symbol == symbol).map(_.id).get)
    _ <- log.info(s"${q.data}")
  } yield ()

}

// xapp-1-A01V9SVTXNH-1999296733059-b6d1e4bb6a291775ea72c561890c76c2faf90e5f56382d3e4cbb897b4381bb9a
