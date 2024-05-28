package molly.core.query

import cats.effect.kernel.Async
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import com.mongodb.client.model.changestream.ChangeStreamDocument
import com.mongodb.client.model.changestream.FullDocument
import com.mongodb.reactivestreams.client.ChangeStreamPublisher
import fs2.Stream
import molly.core.MollyCodec
import molly.core.query.WatchQuery.decodeChangeStreamDocument
import molly.core.reactivestreams.fromStreamPublisher
import org.bson.BsonDocument

final case class WatchQuery[F[_], A] private[core] (
 private[core] val publisher: ChangeStreamPublisher[BsonDocument]
)(implicit
 f: Async[F],
 codec: MollyCodec[F, A]
) {

   /** [[https://mongodb.github.io/mongo-java-driver/4.11/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/ChangeStreamPublisher.html#resumeAfter(org.bson.BsonDocument)]]
     */
   def resumeAfter(resumeToken: BsonDocument): WatchQuery[F, A] = WatchQuery(publisher.resumeAfter(resumeToken))

   /** [[https://mongodb.github.io/mongo-java-driver/4.11/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/ChangeStreamPublisher.html#batchSize(int)]]
     */
   def batchSize(batchSize: Int): WatchQuery[F, A] = WatchQuery(publisher.batchSize(batchSize))

   /** [[https://mongodb.github.io/mongo-java-driver/4.11/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/ChangeStreamPublisher.html#fullDocument(com.mongodb.client.model.changestream.FullDocument)]]
     */
   def fullDocument(fullDocument: FullDocument): WatchQuery[F, A] = WatchQuery(publisher.fullDocument(fullDocument))

   def list(bufferSize: Int = 16): F[List[ChangeStreamDocument[A]]] = stream(bufferSize).compile.toList

   def stream(bufferSize: Int = 16): Stream[F, ChangeStreamDocument[A]] =
      fromStreamPublisher(publisher, bufferSize).evalMap { csd =>
         decodeChangeStreamDocument(csd)
      }
}

object WatchQuery {
   def decodeChangeStreamDocument[F[_], A](csd: ChangeStreamDocument[BsonDocument])(implicit
    f: Async[F],
    codec: MollyCodec[F, A]
   ): F[ChangeStreamDocument[A]] =
      for {
         fd   <- Option(csd.getFullDocument).traverse(codec.decode)
         fdbc <- Option(csd.getFullDocumentBeforeChange).traverse(codec.decode)
      } yield new ChangeStreamDocument(
         csd.getOperationTypeString,
         csd.getResumeToken,
         csd.getNamespaceDocument,
         csd.getDestinationNamespaceDocument,
         fd.getOrElse(null.asInstanceOf[A]),
         fdbc.getOrElse(null.asInstanceOf[A]),
         csd.getDocumentKey,
         csd.getClusterTime,
         csd.getUpdateDescription,
         csd.getTxnNumber,
         csd.getLsid,
         csd.getWallTime,
         csd.getSplitEvent,
         csd.getExtraElements
      )
}
