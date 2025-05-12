package molly.core.query

import cats.effect.kernel.Async
import cats.effect.syntax.spawn.*
import com.mongodb.client.ChangeStreamIterable
import com.mongodb.client.MongoChangeStreamCursor
import com.mongodb.client.model.changestream.ChangeStreamDocument
import com.mongodb.client.model.changestream.FullDocument
import fs2.Stream
import molly.core.MollyCodec
import org.bson.BsonDocument

import scala.concurrent.duration.*

final case class SyncWatchQuery[F[_], A] private[core] (private[core] val iterable: ChangeStreamIterable[BsonDocument])(
    using
    f: Async[F],
    codec: MollyCodec[F, A]
):

    /** [[https://mongodb.github.io/mongo-java-driver/5.5/apidocs/driver-reactive-streams/com/mongodb/reactivestreams/client/ChangeStreamPublisher.html#resumeAfter(org.bson.BsonDocument)]]
      */
    def resumeAfter(resumeToken: BsonDocument): SyncWatchQuery[F, A] = SyncWatchQuery(iterable.resumeAfter(resumeToken))

    /** [[https://mongodb.github.io/mongo-java-driver/5.5/apidocs/driver-reactive-streams/com/mongodb/reactivestreams/client/ChangeStreamPublisher.html#fullDocument(com.mongodb.client.model.changestream.FullDocument)]]
      */
    def fullDocument(fullDocument: FullDocument): SyncWatchQuery[F, A] =
        SyncWatchQuery(iterable.fullDocument(fullDocument))

    def list(bufferSize: Int = 16, timeout: FiniteDuration = 10.seconds): F[List[ChangeStreamDocument[A]]] =
        stream(bufferSize, timeout).compile.toList

    def stream(bufferSize: Int = 16, timeout: FiniteDuration = 10.seconds): Stream[F, ChangeStreamDocument[A]] =
        Stream
            .bracket(f.blocking(iterable.batchSize(bufferSize).cursor()))(cursor => f.blocking(cursor.close()))
            .flatMap(fromCursor(_, bufferSize, timeout))

    private type Cursor = MongoChangeStreamCursor[ChangeStreamDocument[BsonDocument]]

    private def fromCursor(
        cursor: Cursor,
        bufferSize: Int,
        timeout: FiniteDuration
    ): Stream[F, ChangeStreamDocument[A]] =
        def getNext(cursor: Cursor): F[Option[(ChangeStreamDocument[BsonDocument], Cursor)]] =
            f.blocking(if cursor.hasNext() then Some(cursor.next() -> cursor) else None)
                .cancelable(f.blocking(cursor.close()))

        Stream
            .unfoldEval(cursor)(getNext)
            .evalMap(WatchQuery.decodeChangeStreamDocument)
            .groupWithin(bufferSize, timeout)
            .flatMap(Stream.chunk)
