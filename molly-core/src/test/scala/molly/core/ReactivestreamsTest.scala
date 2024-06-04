package molly.core

import cats.effect.IO
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import weaver.SimpleIOSuite

import scala.concurrent.duration.*

object ReactivestreamsTest extends SimpleIOSuite:

  class TestPublisher[A] extends Publisher[A]:

    private var subscribers: Seq[Subscriber[? >: A]] = Seq.empty

    def next(element: A): Unit = subscribers.foreach(s => s.onNext(element))

    def complete(): Unit = subscribers.foreach(s => s.onComplete())

    def error(ex: Throwable): Unit = subscribers.foreach(s => s.onError(ex))

    override def subscribe(s: Subscriber[? >: A]): Unit =
      subscribers = subscribers :+ s

  private val waitSomeMillis: IO[Unit] = IO.sleep(15.millis)

  test("fromSinglePublisher: push value"):
    val pub: TestPublisher[String] = new TestPublisher()

    def push(value: String): IO[Unit] =
      waitSomeMillis >> // Give 'subscribe' a bit time to complete before
        IO(pub.next(value)) >>
        IO(pub.complete())

    val pull: IO[String] = molly.core.reactivestreams.fromSinglePublisher(pub)

    val expected = "test test test"
    push(expected)
      .both(pull)
      .map: (_, obtained) =>
        expect(obtained == expected)

  test("fromSinglePublisher: push error"):
    val pub: TestPublisher[String] = new TestPublisher()

    def push(ex: Exception): IO[Unit] =
      waitSomeMillis >> // Give 'subscribe' a bit time to complete before
        IO(pub.error(ex))

    val pull: IO[String] = molly.core.reactivestreams.fromSinglePublisher(pub)

    val expected = new MollyException("booom")
    push(expected)
      .both(pull)
      .attempt
      .map(_.left.getOrElse(new IllegalStateException("Exception expected")))
      .map(obtained => expect(obtained == expected))

  test("fromSinglePublisher: complete without value"):
    val pub: TestPublisher[String] = new TestPublisher()

    val push: IO[Unit] =
      waitSomeMillis >> // Give 'subscribe' a bit time to complete before
        IO(pub.complete())

    val pull: IO[String] = molly.core.reactivestreams.fromSinglePublisher(pub)

    push
      .both(pull)
      .attempt
      .map(_.left.getOrElse(new IllegalStateException("Exception expected")))
      .map(obtained =>
        expect(obtained.isInstanceOf[MollyException]) && expect(obtained.getMessage.contains("Missing result"))
      )

  test("fromOptionPublisher: push value"):
    val pub: TestPublisher[String] = new TestPublisher()

    def push(value: String): IO[Unit] =
      waitSomeMillis >> // Give 'subscribe' a bit time to complete before
        IO(pub.next(value)) >>
        IO(pub.complete())

    val pull: IO[Option[String]] = molly.core.reactivestreams.fromOptionPublisher(pub)

    val expected = "test test test"
    push(expected)
      .both(pull)
      .map: (_, obtained) =>
        expect(obtained == Some(expected))

  test("fromOptionPublisher: push error"):
    val pub: TestPublisher[String] = new TestPublisher()

    def push(ex: Exception): IO[Unit] =
      waitSomeMillis >> // Give 'subscribe' a bit time to complete before
        IO(pub.error(ex))

    val pull: IO[Option[String]] = molly.core.reactivestreams.fromOptionPublisher(pub)

    val expected = new MollyException("booom")
    push(expected)
      .both(pull)
      .attempt
      .map(_.left.getOrElse(new IllegalStateException("Exception expected")))
      .map(obtained => expect(obtained == expected))

  test("fromOptionPublisher: complete without value"):
    val pub: TestPublisher[String] = new TestPublisher()

    val push: IO[Unit] =
      waitSomeMillis >> // Give 'subscribe' a bit time to complete before
        IO(pub.complete())

    val pull: IO[Option[String]] = molly.core.reactivestreams.fromOptionPublisher(pub)

    push
      .both(pull)
      .map: (_, obtained) =>
        expect(obtained == None)

  test("fromVoidPublisher: push value"):
    val pub: TestPublisher[Void] = new TestPublisher()

    def push: IO[Unit] =
      waitSomeMillis >> // Give 'subscribe' a bit time to complete before
        IO(pub.next(null)) >> // scalafix:ok
        IO(pub.complete())

    val pull: IO[Unit] = molly.core.reactivestreams.fromVoidPublisher(pub)

    push
      .both(pull)
      .map: (_, obtained) =>
        expect(obtained == ())

  test("fromVoidPublisher: push error"):
    val pub: TestPublisher[Void] = new TestPublisher()

    def push(ex: Exception): IO[Unit] =
      waitSomeMillis >> // Give 'subscribe' a bit time to complete before
        IO(pub.error(ex))

    val pull: IO[Unit] = molly.core.reactivestreams.fromVoidPublisher(pub)

    val expected = new MollyException("booom")
    push(expected)
      .both(pull)
      .attempt
      .map(_.left.getOrElse(new IllegalStateException("Exception expected")))
      .map(obtained => expect(obtained == expected))

  test("fromVoidPublisher: complete without value"):
    val pub: TestPublisher[Void] = new TestPublisher()

    val push: IO[Unit] =
      waitSomeMillis >> // Give 'subscribe' a bit time to complete before
        IO(pub.complete())

    val pull: IO[Unit] = molly.core.reactivestreams.fromVoidPublisher(pub)

    push
      .both(pull)
      .map: (_, obtained) =>
        expect(obtained == ())
