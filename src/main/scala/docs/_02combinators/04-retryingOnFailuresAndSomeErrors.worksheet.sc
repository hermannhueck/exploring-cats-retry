import cats.effect.IO
import cats.effect.unsafe.implicits.global
import retry._

import java.util.concurrent.TimeoutException

val httpClient = util.FlakyHttpClient()

val flakyRequest: IO[String] = IO(httpClient.getRecordDetails("foo"))

def isTimeoutException(e: Throwable): IO[Boolean] = e match {
  case _: TimeoutException => IO.pure(true)
  case _                   => IO.pure(false)
}

/*
def retryingOnFailuresAndSomeErrors[M[_]: Sleep, A, E](policy: RetryPolicy[M],
                                                       wasSuccessful: A => M[Boolean],
                                                       isWorthRetrying: E => M[Boolean],
                                                       onFailure: (A, RetryDetails) => M[Unit],
                                                       onError: (E, RetryDetails) => M[Unit])
                                                      (action: => M[A])
                                                      (implicit ME: MonadError[M, E]): M[A]
 */

val io = retryingOnFailuresAndSomeErrors(
  wasSuccessful = (s: String) => IO.pure(s != "pending"),
  isWorthRetrying = isTimeoutException,
  policy = RetryPolicies.limitRetries[IO](5),
  onFailure = retry.noop[IO, String],
  onError = retry.noop[IO, Throwable]
)(flakyRequest)

io.unsafeRunSync()

import retry.syntax.all._

// To retry only on errors and results that are worth retrying
IO(httpClient.getRecordDetails("foo"))
  .retryingOnFailuresAndSomeErrors(
    wasSuccessful = (s: String) => IO.pure(s != "pending"),
    isWorthRetrying = isTimeoutException,
    policy = RetryPolicies.limitRetries[IO](2),
    onFailure = retry.noop[IO, String],
    onError = retry.noop[IO, Throwable]
  )
  .unsafeRunSync()
