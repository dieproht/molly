package molly.core.query

import cats.effect.kernel.Async
import com.mongodb.reactivestreams.client.AggregatePublisher
import fs2.Stream
import molly.core.reactivestreams.fromOptionPublisher
import molly.core.reactivestreams.fromStreamPublisher
import org.bson.BsonDocument

final case class AggregateQuery[F[_]: Async] private[core] (
 private[core] val publisher: AggregatePublisher[BsonDocument]
) {

   def first: F[Option[BsonDocument]] = fromOptionPublisher(publisher.first)

   def list: F[List[BsonDocument]] = stream.compile.toList

   def stream: Stream[F, BsonDocument] = fromStreamPublisher(publisher, bufferSize = 1)
}
