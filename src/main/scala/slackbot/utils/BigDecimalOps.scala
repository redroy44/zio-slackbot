package slackbot.utils

object BigDecimalOps {
  implicit class BigDecimalOps(bd: BigDecimal) {
    def pretty: String = bd.setScale(2, scala.math.BigDecimal.RoundingMode.UP).toString
  }
}
