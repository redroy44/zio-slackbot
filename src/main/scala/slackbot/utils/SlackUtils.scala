package slackbot.utils

import zhttp.http._
// import zio._

import java.time.Instant

object SlackUtils {
  def validated[R, E](
    secret: String
  )(fail: HttpApp[R, E], success: HttpApp[R, E]): HttpApp[R, E] =
    Http.flatten {
      Http.fromFunction[Request] { req =>
        (for {
          slackTimestamp <- req.getHeaderValue("X-Slack-Request-Timestamp")
          slackSignature <- req.getHeaderValue("X-Slack-Signature")
          reqBody        <- req.getBodyAsString
          signature = "v0:" + slackTimestamp + ":" + reqBody
          signed    = "v0=" + HMACgen.generateHMAC(secret, signature)
          if (slackSignature
            .equals(signed) && (math.abs(Instant.now.toEpochMilli - (slackTimestamp.toLong) * 1000) < 60 * 5 * 1000))
        } yield ()).fold(fail)(_ => success)
      }
    }
}

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HMACgen {

  def generateHMAC(sharedSecret: String, preHashString: String): String = {
    val secret = new SecretKeySpec(sharedSecret.getBytes("UTF-8"), "HmacSHA256") //Crypto Funs : 'SHA256' , 'HmacSHA1'
    val mac    = Mac.getInstance("HmacSHA256")
    mac.init(secret)
    val hashString: Array[Byte] = mac.doFinal(preHashString.getBytes("UTF-8"))
    hashString.map("%02x".format(_)).mkString
  }

}
