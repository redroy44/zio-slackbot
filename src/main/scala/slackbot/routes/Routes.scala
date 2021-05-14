package slackbot.routes

import zio._
import zio.logging._
import zhttp.http._

import slackbot.service.CommandProcessor
import slackbot.client.CoinMarketCapClient
import sttp.client3.httpclient.zio._
import sttp.model.Uri
import java.net.URLDecoder

import slackbot.model._

object Routes {
  def slack: HttpApp[Logging with Has[CoinMarketCapClient] with SttpClient with Has[
    CommandProcessor
  ], Throwable] =
    HttpApp.collectM {
      case req @ Method.POST -> Root / "slack" / "echo" =>
        handleRequest(req)

      case req @ Method.POST -> Root / "slack" / "check" =>
        for {
          _    <- log.info(s"received request on ${req.endpoint}")
          form <- extractFormData(req)
          _    <- log.info(form.toString())
          _ <- CommandProcessor.enqueueCommand(
            CheckCommand(
              args = form("text"),
              responseUrl = Uri.parse(form("response_url")).getOrElse(Uri("example.com"))
            )
          )
        } yield Response.ok
    }

  private def handleRequest(req: Request): URIO[Logging with Has[CommandProcessor], UResponse] =
    for {
      form <- extractFormData(req)
      _ <- CommandProcessor.enqueueCommand(
        EchoCommand(
          args = form("text"),
          responseUrl = Uri.parse(form("response_url")).getOrElse(Uri("example.com"))
        )
      )
    } yield Response.ok

  private def extractFormData(r: Request): UIO[Map[String, String]] = ZIO.getOrFail {
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
  } catchAll (_ => ZIO.succeed(Map[String, String]()))
}
