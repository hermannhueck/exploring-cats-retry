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

def isWorthRetrying(error: AppError): Effect[Boolean] =
  EitherT.pure(error.reason.contains("Boom!"))

def logError[F[_]: Sync](error: AppError, details: RetryDetails): F[Unit] =
  Sync[F].delay(println(s"Raised error $error. Details $details"))

val policy = RetryPolicies.limitRetries[Effect](2)

/*
def retryingOnSomeErrors[M[_]: Monad: Sleep, A, E: Handle[M, *]](
  policy: RetryPolicy[M],
  isWorthRetrying: E => M[Boolean],
  onError: (E, RetryDetails) => M[Unit]
)(action: => M[A]): M[A]
 */

retry
  .mtl
  .retryingOnSomeErrors(policy, isWorthRetrying, logError[Effect])(failingOperation[Effect])
  .value
  .unsafeRunTimed(1.second)
