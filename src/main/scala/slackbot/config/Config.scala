package slackbot.config

import zio._
import zio.config._
import zio.config.ConfigDescriptor._
// import zio.config.magnolia._

case class Config(cmcApiKey: String, slackSigningSecret: String, port: Int)

object Config {
  val descriptor: ConfigDescriptor[Config] =
    (string("cmcApiKey") |@| string("slackSigningSecret") |@| int("PORT"))(Config.apply, Config.unapply)

  val live: ZLayer[system.System, Nothing, Has[Config]] =
    (ZConfig.fromPropertiesFile("application.conf", descriptor) <> ZConfig.fromSystemEnv(descriptor)).orDie
}
