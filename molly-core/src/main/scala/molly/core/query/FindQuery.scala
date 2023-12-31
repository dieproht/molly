package molly.core.query

import cats.effect.kernel.Async
import com.mongodb.reactivestreams.client.FindPublisher
import fs2.Stream
import molly.core.reactivestreams.fromOptionPublisher
import molly.core.reactivestreams.fromStreamPublisher
import org.bson.BsonDocument
import org.bson.conversions.Bson

final case class FindQuery[F[_]: Async] private[core] (private[core] val publisher: FindPublisher[BsonDocument]) {

   def filter(filter: Bson): FindQuery[F] = FindQuery(publisher.filter(filter))

   def limit(limit: Int): FindQuery[F] = FindQuery(publisher.limit(limit))

   def first: F[Option[BsonDocument]] = fromOptionPublisher(publisher.first)

   def list: F[List[BsonDocument]] = stream.compile.toList

   def stream: Stream[F, BsonDocument] = fromStreamPublisher(publisher, bufferSize = 1)
}
