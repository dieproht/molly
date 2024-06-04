package molly.core.query

import cats.effect.kernel.Async
import cats.effect.kernel.Sync
import cats.effect.syntax.spawn.*
import com.mongodb.client.ChangeStreamIterable
import com.mongodb.client.MongoChangeStreamCursor
import com.mongodb.client.model.changestream.ChangeStreamDocument
import com.mongodb.client.model.changestream.FullDocument
import fs2.Chunk
import fs2.Stream
import molly.core.MollyCodec
import org.bson.BsonDocument

final case class SyncWatchQuery[F[_], A] private[core] (private[core] val iterable: ChangeStreamIterable[BsonDocument])(
    using
    f: Async[F],
    codec: MollyCodec[F, A]
):

  /** [[https://mongodb.github.io/mongo-java-driver/5.1/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/ChangeStreamPublisher.html#resumeAfter(org.bson.BsonDocument)]]
    */
  def resumeAfter(resumeToken: BsonDocument): SyncWatchQuery[F, A] = SyncWatchQuery(iterable.resumeAfter(resumeToken))

  /** [[https://mongodb.github.io/mongo-java-driver/5.1/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/ChangeStreamPublisher.html#fullDocument(com.mongodb.client.model.changestream.FullDocument)]]
    */
  def fullDocument(fullDocument: FullDocument): SyncWatchQuery[F, A] =
    SyncWatchQuery(iterable.fullDocument(fullDocument))

  def list(bufferSize: Int = 16): F[List[ChangeStreamDocument[A]]] = stream(bufferSize).compile.toList

  def stream(bufferSize: Int = 16): Stream[F, ChangeStreamDocument[A]] =
    Stream
      .bracket(f.delay(iterable.batchSize(bufferSize).cursor()))(cursor => f.delay(cursor.close()))
      .flatMap(fromCursor(_, bufferSize))

  private type Cursor = MongoChangeStreamCursor[ChangeStreamDocument[BsonDocument]]

  private def fromCursor(cursor: Cursor, bufferSize: Int): Stream[F, ChangeStreamDocument[A]] =
    def getNextChunk(cursor: Cursor): F[Option[(Chunk[ChangeStreamDocument[BsonDocument]], Cursor)]] =
      f
        .suspend(Sync.Type.Blocking):
          val bldr = Vector.newBuilder[ChangeStreamDocument[BsonDocument]]
          var cnt = 0
          while cnt < bufferSize && cursor.hasNext do
            bldr += cursor.next()
            cnt += 1
          if cnt == 0 then None else Some((Chunk.from(bldr.result()), cursor))
        .cancelable(f.delay(cursor.close()))

    Stream
      .unfoldChunkEval(cursor)(getNextChunk)
      .evalMap(WatchQuery.decodeChangeStreamDocument)
