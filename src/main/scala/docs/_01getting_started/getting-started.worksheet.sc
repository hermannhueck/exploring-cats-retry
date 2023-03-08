import cats.effect.IO
import scala.concurrent.duration.FiniteDuration
import retry._
import retry.RetryDetails._

val httpClient = util.FlakyHttpClient()

val flakyRequest: IO[String] = IO {
  httpClient.getCatGif()
}

val retryFiveTimes = RetryPolicies.limitRetries[IO](5)

val logMessages = collection.mutable.ArrayBuffer.empty[String]

@annotation.nowarn("cat=unused")
def logError(err: Throwable, details: RetryDetails): IO[Unit] = details match {

  case WillDelayAndRetry(nextDelay: FiniteDuration, retriesSoFar: Int, cumulativeDelay: FiniteDuration) =>
    IO {
      logMessages.append(s"Failed to download. So far we have retried $retriesSoFar times.")
    }

  case GivingUp(totalRetries: Int, totalDelay: FiniteDuration) =>
    IO {
      logMessages.append(s"Giving up after $totalRetries retries")
    }
}

import cats.effect.unsafe.implicits.global

val flakyRequestWithRetry: IO[String] =
  retryingOnAllErrors[String](
    policy = RetryPolicies.limitRetries[IO](5),
    onError = logError
  )(flakyRequest)

flakyRequestWithRetry.unsafeRunSync()

logMessages.foreach(println)
