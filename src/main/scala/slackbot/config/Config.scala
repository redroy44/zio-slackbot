package slackbot.config

import zio._
import zio.config._
import zio.config.ConfigDescriptor._
import zio.duration._

// import zio.config.magnolia._

case class Config(cmcApiKey: String, slackSigningSecret: String, port: Int, cryptoRefreshPeriod: Duration)

object Config {
  val descriptor: ConfigDescriptor[Config] =
    (string("CMC_API_KEY") |@| string("SLACK_SIGNING_SECRET") |@| int("PORT") |@| zioDuration("CRYPTO_REFRESH_PERIOD")
      .default(60.minutes))(
      Config.apply,
      Config.unapply
    )

  val live: ZLayer[system.System, Nothing, Has[Config]] =
    (ZConfig.fromPropertiesFile("application.conf", descriptor) <> ZConfig.fromSystemEnv(descriptor)).orDie
}
