package molly.core

import cats.effect.kernel.Async
import org.bson.BsonDocument

/** An implementation of [[molly.core.MollyCodec]] for [[org.bson.BsonDocument]] (identity mapping).
  */
object bsondocument {

   implicit def bsonDocumentCodec[F[_]: Async]: MollyCodec[F, BsonDocument] = new MollyCodec[F, BsonDocument] {
      override def decode(doc: BsonDocument): F[BsonDocument] = Async[F].pure(doc)
      override def encode(obj: BsonDocument): F[BsonDocument] = Async[F].pure(obj)
   }

   type BsonDocumentCollection[F[_]] = MollyCollection[F, BsonDocument]
}
