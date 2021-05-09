package slackbot

import zio.test._
import zio.test.Assertion._

object MainSpec extends DefaultRunnableSpec {

  def spec = suite("Test environment")(
    test("expect call with input satisfying assertion") {
      assert(40 + 2)(equalTo(42))
    }
  )
}
