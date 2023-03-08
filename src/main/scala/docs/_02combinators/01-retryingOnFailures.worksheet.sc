import cats.effect.IO
import cats.effect.unsafe.implicits.global
import retry._

import scala.concurrent.duration._

val policy = RetryPolicies.constantDelay[IO](10.milliseconds)

@annotation.nowarn("cat=unused")
def onFailure(failedValue: Int, details: RetryDetails): IO[Unit] = {
  IO(println(s"Rolled a $failedValue, retrying ..."))
}

val loadedDie = util.LoadedDie(2, 5, 4, 1, 3, 2, 6)

/*
def retryingOnFailures[M[_]: Monad: Sleep, A](policy: RetryPolicy[M],
                                              wasSuccessful: A => M[Boolean],
                                              onFailure: (A, RetryDetails) => M[Unit])
                                              (action: => M[A]): M[A]
 */

val io = retryingOnFailures(policy, (i: Int) => IO.pure(i == 6), onFailure) {
  IO(loadedDie.roll())
}

io.unsafeRunSync()

import retry.syntax.all._

// To retry until you get a value you like
IO(loadedDie.roll())
  .retryingOnFailures(
    policy = RetryPolicies.limitRetries[IO](2),
    wasSuccessful = (i: Int) => IO.pure(i == 6),
    onFailure = retry.noop[IO, Int]
  )
  .unsafeRunSync()
