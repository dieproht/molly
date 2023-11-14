package molly.core

import cats.effect.kernel.Async
import org.bson.BsonDocument

/** Interface for mapping a data type A to a collection.
  */
abstract class MollyCodec[F[_]: Async, A] {
   def decode(doc: BsonDocument): F[A]
   def encode(obj: A): F[BsonDocument]
}
