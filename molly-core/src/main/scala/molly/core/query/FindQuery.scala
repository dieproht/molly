package molly.core.query

import cats.effect.kernel.Async
import com.mongodb.reactivestreams.client.FindPublisher
import fs2.Stream
import molly.core.reactivestreams.fromPublisher
import org.bson.BsonDocument

final case class FindQuery[F[_]: Async] private[core] (private[core] val publisher: FindPublisher[BsonDocument]) {

   def list: F[List[BsonDocument]] = stream.compile.toList

   def stream: Stream[F, BsonDocument] = fromPublisher(publisher, bufferSize = 1)
}
