package molly.core.query

import cats.effect.kernel.Async
import com.mongodb.reactivestreams.client.AggregatePublisher
import fs2.Stream
import molly.core.reactivestreams.fromOptionPublisher
import molly.core.reactivestreams.fromStreamPublisher
import org.bson.BsonDocument

final case class AggregateQuery[F[_]] private[core] (
    private[core] val publisher: AggregatePublisher[BsonDocument]
)(using Async[F]):

  def first(): F[Option[BsonDocument]] = fromOptionPublisher(publisher.first)

  def list(bufferSize: Int = 16): F[List[BsonDocument]] = stream(bufferSize).compile.toList

  def stream(bufferSize: Int = 16): Stream[F, BsonDocument] = fromStreamPublisher(publisher, bufferSize)
