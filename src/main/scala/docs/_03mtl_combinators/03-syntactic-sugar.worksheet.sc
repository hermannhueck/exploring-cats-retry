import retry._
import cats.data.EitherT
import cats.effect.{IO, Sync}
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.mtl.Handle
import retry.mtl.syntax.all._
import retry.syntax.all._
import scala.concurrent.duration._
import cats.effect.unsafe.implicits.global

case class AppError(reason: String)

class Service[F[_]: Sleep](client: util.FlakyHttpClient)(implicit F: Sync[F], AH: Handle[F, AppError]) {

  // evaluates retry exclusively on errors produced by Handle.
  def findCoolCatGifRetryMtl(policy: RetryPolicy[F]): F[String] =
    findCoolCatGif.retryingOnAllMtlErrors[AppError](policy, logMtlError)

  // evaluates retry on errors produced by MonadError and Handle
  def findCoolCatGifRetryAll(policy: RetryPolicy[F]): F[String] =
    findCoolCatGif
      .retryingOnAllErrors(policy, logError)
      .retryingOnAllMtlErrors[AppError](policy, logMtlError)

  private def findCoolCatGif: F[String] =
    for {
      gif <- findCatGif
      _   <- isCoolGif(gif)
    } yield gif

  private def findCatGif: F[String] =
    F.delay(client.getCatGif())

  private def isCoolGif(string: String): F[Unit] =
    if (string.contains("cool")) F.unit
    else AH.raise(AppError("Gif is not cool"))

  private def logError(error: Throwable, details: RetryDetails): F[Unit] =
    F.delay(println(s"Raised error $error. Details $details"))

  private def logMtlError(error: AppError, details: RetryDetails): F[Unit] =
    F.delay(println(s"Raised MTL error $error. Details $details"))
}

type Effect[A] = EitherT[IO, AppError, A]

val policy = RetryPolicies.limitRetries[Effect](5)

val service = new Service[Effect](util.FlakyHttpClient())

service.findCoolCatGifRetryMtl(policy).value.attempt.unsafeRunTimed(1.second)

service.findCoolCatGifRetryAll(policy).value.attempt.unsafeRunTimed(1.second)
