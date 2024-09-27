package molly.core.query

import cats.effect.kernel.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import com.mongodb.reactivestreams.client.FindPublisher
import fs2.Stream
import molly.core.MollyCodec
import molly.core.reactivestreams.fromOptionPublisher
import molly.core.reactivestreams.fromStreamPublisher
import org.bson.BsonDocument
import org.bson.conversions.Bson

final case class FindQuery[F[_], A] private[core] (private[core] val publisher: FindPublisher[BsonDocument])(using
    f: Async[F],
    codec: MollyCodec[F, A]
):

  /** [[https://mongodb.github.io/mongo-java-driver/5.2/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/FindPublisher.html#filter(org.bson.conversions.Bson)]]
    */
  def filter(filter: Bson): FindQuery[F, A] = FindQuery(publisher.filter(filter))

  /** [[https://mongodb.github.io/mongo-java-driver/5.2/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/FindPublisher.html#limit(int)]]
    */
  def limit(limit: Int): FindQuery[F, A] = FindQuery(publisher.limit(limit))

  /** [[https://mongodb.github.io/mongo-java-driver/5.2/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/FindPublisher.html#sort(org.bson.conversions.Bson)]]
    */
  def sort(sort: Bson): FindQuery[F, A] = FindQuery(publisher.sort(sort))

  def first(): F[Option[A]] =
    for
      resultDoc <- fromOptionPublisher(publisher.first)
      result    <- resultDoc.traverse(codec.decode)
    yield result

  def list(bufferSize: Int = 16): F[List[A]] = stream(bufferSize).compile.toList

  def stream(bufferSize: Int = 16): Stream[F, A] = fromStreamPublisher(publisher, bufferSize).evalMap(codec.decode)
