import retry.{RetryDetails, RetryPolicies}
import cats.data.EitherT
import cats.effect.{IO, Sync}
import cats.mtl.Handle
import scala.concurrent.duration._
import cats.effect.unsafe.implicits.global

type Effect[A] = EitherT[IO, AppError, A]

case class AppError(reason: String)

def failingOperation[F[_]: Handle[*[_], AppError]]: F[Unit] =
  Handle[F, AppError].raise(AppError("Boom!"))

def logError[F[_]: Sync](error: AppError, details: RetryDetails): F[Unit] =
  Sync[F].delay(println(s"Raised error $error. Details $details"))

val policy = RetryPolicies.limitRetries[Effect](2)

/*
 def retryingOnSomeErrors[M[_]: Monad: Sleep, A, E: Handle[M, *]](
  policy: RetryPolicy[M],
  onError: (E, RetryDetails) => M[Unit]
)(action: => M[A]): M[A]
 */

retry
  .mtl
  .retryingOnAllErrors(policy, logError[Effect])(failingOperation[Effect])
  .value
  .unsafeRunTimed(1.second)
