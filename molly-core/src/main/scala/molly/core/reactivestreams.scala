package molly.core

import cats.effect.kernel.Async
import fs2.Stream
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/** Interoperability layer between [[https://www.reactive-streams.org Reactive Streams]] and
  * [[https://typelevel.org/cats-effect Cats Effect]] resp. [[https://fs2.io FS2]].
  */
object reactivestreams:

  def fromStreamPublisher[F[_], A](publisher: Publisher[A], bufferSize: Int)(using Async[F]): Stream[F, A] =
    fs2.interop.reactivestreams.fromPublisher(publisher, bufferSize)

  def fromSinglePublisher[F[_], A](publisher: Publisher[A])(using f: Async[F]): F[A] =
    type Callback = Either[Throwable, A] => Unit
    type Effect = Callback => Unit
    val effect: Effect = (callback: Callback) =>
      publisher.subscribe:
        new Subscriber[A]:
          private var result: Option[A] = None
          override def onComplete(): Unit =
            callback(result.toRight(MollyException("Missing result when completing fromSinglePublisher")))
          override def onError(error: Throwable): Unit = callback(Left(error))
          override def onNext(res: A): Unit = result = Option(res)
          override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
    f.async_(effect)

  def fromOptionPublisher[F[_], A](publisher: Publisher[A])(using f: Async[F]): F[Option[A]] =
    type Callback = Either[Throwable, Option[A]] => Unit
    type Effect = Callback => Unit
    val effect: Effect = (callback: Callback) =>
      publisher.subscribe:
        new Subscriber[A]:
          private var result: Option[A] = None
          override def onComplete(): Unit = callback(Right(result))
          override def onError(error: Throwable): Unit = callback(Left(error))
          override def onNext(res: A): Unit = result = Option(res)
          override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
    f.async_(effect)

  def fromVoidPublisher[F[_]](publisher: Publisher[Void])(using f: Async[F]): F[Unit] =
    type Callback = Either[Throwable, Unit] => Unit
    type Effect = Callback => Unit
    val effect: Effect = (callback: Callback) =>
      publisher.subscribe:
        new Subscriber[Void]:
          override def onComplete(): Unit = callback(Right(()))
          override def onError(error: Throwable): Unit = callback(Left(error))
          override def onNext(res: Void): Unit = ()
          override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
    f.async_(effect)
