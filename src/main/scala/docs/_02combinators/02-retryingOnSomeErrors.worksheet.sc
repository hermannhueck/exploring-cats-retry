import cats.effect.IO
import cats.effect.unsafe.implicits.global
import retry._

import java.io.IOException

val httpClient = util.FlakyHttpClient()

val flakyRequest: IO[String] = IO(httpClient.getCatGif())

def isIOException(e: Throwable): IO[Boolean] = e match {
  case _: IOException => IO.pure(true)
  case _              => IO.pure(false)
}

/*
def retryingOnSomeErrors[M[_]: Sleep, A, E](policy: RetryPolicy[M],
                                            isWorthRetrying: E => M[Boolean],
                                            onError: (E, RetryDetails) => M[Unit])
                                           (action: => M[A])
                                           (implicit ME: MonadError[M, E]): M[A]
 */

val io = retryingOnSomeErrors(
  isWorthRetrying = isIOException,
  policy = RetryPolicies.limitRetries[IO](5),
  onError = retry.noop[IO, Throwable]
)(flakyRequest)

io.unsafeRunSync()

import retry.syntax.all._

// To retry only on errors that are worth retrying
IO(httpClient.getCatGif())
  .retryingOnSomeErrors(
    isWorthRetrying = isIOException,
    policy = RetryPolicies.limitRetries[IO](2),
    onError = retry.noop[IO, Throwable]
  )
  .unsafeRunSync()
