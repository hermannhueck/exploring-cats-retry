import cats.effect.IO
import cats.effect.unsafe.implicits.global
import retry._

val httpClient = util.FlakyHttpClient()

val flakyRequest: IO[String] = IO(httpClient.getRecordDetails("foo"))

/*
def retryingOnFailuresAndAllErrors[M[_]: Sleep, A, E](policy: RetryPolicy[M],
                                                      wasSuccessful: A => M[Boolean],
                                                      onFailure: (A, RetryDetails) => M[Unit],
                                                      onError: (E, RetryDetails) => M[Unit])
                                                     (action: => M[A])
                                                     (implicit ME: MonadError[M, E]): M[A]
 */

val io = retryingOnFailuresAndAllErrors(
  wasSuccessful = (s: String) => IO.pure(s != "pending"),
  policy = RetryPolicies.limitRetries[IO](5),
  onFailure = retry.noop[IO, String],
  onError = retry.noop[IO, Throwable]
)(flakyRequest)

io.unsafeRunSync()

import retry.syntax.all._

// To retry all errors and results that are worth retrying
IO(httpClient.getRecordDetails("foo"))
  .retryingOnFailuresAndAllErrors(
    wasSuccessful = (s: String) => IO.pure(s != "pending"),
    policy = RetryPolicies.limitRetries[IO](2),
    onFailure = retry.noop[IO, String],
    onError = retry.noop[IO, Throwable]
  )
  .unsafeRunSync()
