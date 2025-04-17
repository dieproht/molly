package molly.core

import cats.effect.kernel.Async
import org.bson.BsonDocument

/** Interface for mapping a data type A to a collection.
  */
trait MollyCodec[F[_], A](using Async[F]):
    def decode(doc: BsonDocument): F[A]
    def encode(obj: A): F[BsonDocument]

object MollyCodec:

    given bsonDocumentCodec[F[_]](using f: Async[F]): MollyCodec[F, BsonDocument] =
        new MollyCodec[F, BsonDocument]:
            override def decode(doc: BsonDocument): F[BsonDocument] = f.pure(doc)
            override def encode(obj: BsonDocument): F[BsonDocument] = f.pure(obj)
