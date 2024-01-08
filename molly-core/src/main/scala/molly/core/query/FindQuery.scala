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

final case class FindQuery[F[_], A] private[core] (private[core] val publisher: FindPublisher[BsonDocument])(implicit
 f: Async[F],
 codec: MollyCodec[F, A]
) {

   def filter(filter: Bson): FindQuery[F, A] = FindQuery(publisher.filter(filter))

   def limit(limit: Int): FindQuery[F, A] = FindQuery(publisher.limit(limit))

   def first: F[Option[A]] =
      for {
         resultDoc <- fromOptionPublisher(publisher.first)
         result    <- resultDoc.traverse(codec.decode)
      } yield result

   def list(bufferSize: Int = 16): F[List[A]] = stream(bufferSize).compile.toList

   def stream(bufferSize: Int = 16): Stream[F, A] = fromStreamPublisher(publisher, bufferSize).evalMap(codec.decode)
}
