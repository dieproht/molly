package molly.core

import cats.effect.kernel.Async
import fs2.Stream
import fs2.interop.reactivestreams.fromPublisher
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/** Interoperability layer between [[https://www.reactive-streams.org Reactive Streams]] and
  * [[https://typelevel.org/cats-effect Cats Effect]] resp. [[https://fs2.io FS2]].
  */
object reactivestreams {

   def fromStreamPublisher[F[_]: Async, A](pub: Publisher[A], bufferSize: Int): Stream[F, A] =
      fs2.interop.reactivestreams.fromPublisher(pub, bufferSize)

   def fromSinglePublisher[F[_]: Async, A](pub: Publisher[A]): F[A] =
      Async[F].async_((callback: Either[Throwable, A] => Unit) =>
         pub.subscribe(new Subscriber[A] {
            private var result: Option[A] = None
            override def onComplete(): Unit =
               callback(result.toRight(MollyException("Missing result when completing publisher")))
            override def onError(err: Throwable): Unit = callback(Left(err))
            override def onNext(res: A): Unit = result = Option(res)
            override def onSubscribe(sub: Subscription): Unit = sub.request(1)
         })
      )

   def fromOptionPublisher[F[_]: Async, A](pub: Publisher[A]): F[Option[A]] =
      Async[F].async_((callback: Either[Throwable, Option[A]] => Unit) =>
         pub.subscribe(new Subscriber[A] {
            private var result: Option[A] = None
            override def onComplete(): Unit = callback(Right(result))
            override def onError(err: Throwable): Unit = callback(Left(err))
            override def onNext(res: A): Unit = result = Option(res)
            override def onSubscribe(sub: Subscription): Unit = sub.request(1)
         })
      )
}
