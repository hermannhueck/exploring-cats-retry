import cats.effect.IO
import cats.effect.unsafe.implicits.global
import retry._

val httpClient = util.FlakyHttpClient()

val flakyRequest: IO[String] = IO(httpClient.getCatGif())

/*
def retryingOnAllErrors[M[_]: Sleep, A, E](policy: RetryPolicy[M],
                                           onError: (E, RetryDetails) => M[Unit])
                                          (action: => M[A])
                                          (implicit ME: MonadError[M, E]): M[A]
 */

val io = retryingOnAllErrors(
  policy = RetryPolicies.limitRetries[IO](5),
  onError = retry.noop[IO, Throwable]
)(flakyRequest)

io.unsafeRunSync()

import retry.syntax.all._

// To retry on all errors
IO(httpClient.getCatGif())
  .retryingOnAllErrors(
    policy = RetryPolicies.limitRetries[IO](2),
    onError = retry.noop[IO, Throwable]
  )
  .unsafeRunSync()
