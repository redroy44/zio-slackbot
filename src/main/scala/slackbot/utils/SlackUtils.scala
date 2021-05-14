package slackbot.utils

import zhttp.http._
import zio._
import zio.logging._
import zio.crypto.mac._

import java.nio.charset.StandardCharsets._

import java.time.Instant

object SlackUtils {
  def validated[R <: Logging with HMAC.HMAC](
    secret: String
  )(fail: RHttpApp[R], success: RHttpApp[R]): RHttpApp[R] =
    Http.flatten {
      Http.fromEffectFunction { req =>
        (for {
          (signature, sls, ts) <- extractSignature(req)
          secretKey            <- HMAC.deserializeKey(HMACSerializedKey(s"HmacSHA256-${secret.getBytes("UTF-8")}"))

          hmac     <- HMAC.sign(signature, secretKey.get, UTF_8)
          verified <- HMAC.verify(secret, hmac, secretKey.get, UTF_8)
          _        <- log.info(s"${signature}, ${sls}, ${ts}, ${hmac} - ${check(verified, ts)}")
          eff      <- ZIO.succeed(if (check(verified, ts)) success else fail)
        } yield eff).catchSome { case _: NoSuchElementException => ZIO.succeed(fail) }
      // (for {
      //   slackTimestamp <- req.getHeaderValue("X-Slack-Request-Timestamp")
      //   slackSignature <- req.getHeaderValue("X-Slack-Signature")
      //   reqBody        <- req.getBodyAsString
      //   signature = "v0:" + slackTimestamp + ":" + reqBody
      //   signed    = "v0=" + HMACgen.generateHMAC(secret, signature)
      //   if (slackSignature
      //     .equals(signed) && (math.abs(Instant.now.toEpochMilli - (slackTimestamp.toLong) * 1000) < 60 * 5 * 1000))
      // } yield ()).fold(fail)(_ => success)
      }
    }

  def extractSignature(req: Request): Task[(String, String, String)] = ZIO.getOrFail {
    for {
      slackTimestamp <- req.getHeaderValue("X-Slack-Request-Timestamp")
      slackSignature <- req.getHeaderValue("X-Slack-Signature")
      reqBody        <- req.getBodyAsString
      signature = "v0:" + slackTimestamp + ":" + reqBody
    } yield (signature, slackSignature, slackTimestamp)
  }

  private def check(verified: Boolean, slackTimestamp: String): Boolean =
    verified && (math.abs(Instant.now.toEpochMilli - (slackTimestamp.toLong) * 1000) < 60 * 5 * 1000)
}

// import javax.crypto.Mac
// import javax.crypto.spec.SecretKeySpec

// object HMACgen {

//   def generateHMAC(sharedSecret: String, preHashString: String): String = {
//     val secret = new SecretKeySpec(sharedSecret.getBytes("UTF-8"), "HmacSHA256") //Crypto Funs : 'SHA256' , 'HmacSHA1'
//     val mac    = Mac.getInstance("HmacSHA256")
//     mac.init(secret)
//     val hashString: Array[Byte] = mac.doFinal(preHashString.getBytes("UTF-8"))
//     hashString.map("%02x".format(_)).mkString
//   }

// }
