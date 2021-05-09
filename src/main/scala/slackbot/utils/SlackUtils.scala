package slackbot.utils

import zhttp.http._
import zio._

import java.time.Instant

object SlackUtils {
  def validateRequest[R](
    request: Request
  )(f: Request => URIO[R, UResponse]): URIO[R, UResponse] = {
    val secret = "3f3368dfbb5b08b42e5d9c251099a3d7"

    val ts = request.headers.find(_.name == "X-Slack-Request-Timestamp").get

    val slackSignature = request.headers.find(_.name == "X-Slack-Signature").get.value.toString
    val request_body   = request.getBodyAsString.getOrElse("")
    val mySignature    = "v0:" + ts.value.toString() + ":" + request_body
    val signed         = "v0=" + HMACgen.generateHMAC(secret, mySignature)

    if (
      slackSignature
        .equals(signed) && (math.abs(Instant.now.toEpochMilli - (ts.value.toString().toLong) * 1000) < 60 * 5 * 1000)
    )
      f(request)
    else
      UIO(HttpError.Unauthorized("Request verification failed").toResponse)
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
