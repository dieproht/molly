package molly.core.query

import cats.effect.kernel.Async
import com.mongodb.client.model.changestream.ChangeStreamDocument
import com.mongodb.reactivestreams.client.ChangeStreamPublisher
import fs2.Stream
import molly.core.reactivestreams.fromPublisher
import org.bson.BsonDocument

final case class WatchQuery[F[_]: Async] private[core] (
 private[core] val publisher: ChangeStreamPublisher[BsonDocument]
) {

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/ChangeStreamPublisher.html#resumeAfter(org.bson.BsonDocument)]]
     */
   def resumeAfter(resumeToken: BsonDocument): WatchQuery[F] = WatchQuery(publisher.resumeAfter(resumeToken))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/ChangeStreamPublisher.html#batchSize(int)]]
     */
   def batchSize(batchSize: Int): WatchQuery[F] = WatchQuery(publisher.batchSize(batchSize))

   def list: F[List[ChangeStreamDocument[BsonDocument]]] = stream.compile.toList

   def stream: Stream[F, ChangeStreamDocument[BsonDocument]] = fromPublisher(publisher, bufferSize = 1)
}
