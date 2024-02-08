package molly.core

import cats.effect.kernel.Async
import org.bson.BsonValue

/** An implementation of [[molly.core.MollyCodec]] for [[org.bson.BsonDocument]] (identity mapping).
  */
object bsondocument {

   implicit def bsonDocumentCodec[F[_]: Async]: MollyCodec[F, BsonDocument] = new MollyCodec[F, BsonDocument] {
      override def decode(doc: BsonDocument): F[BsonDocument] = Async[F].pure(doc)
      override def encode(obj: BsonDocument): F[BsonDocument] = Async[F].pure(obj)
   }

   type BsonDocumentCollection[F[_]] = MollyCollection[F, BsonDocument]

   type BsonDocument = org.bson.BsonDocument

   /** Convenience layer over [[org.bson.BsonDocument the Java driver's BsonDocument class]] and an implementation of
     * [[molly.core.MollyCodec]] for [[org.bson.BsonDocument]] (identity mapping).
     */
   object BsonDocument {
      def apply(): BsonDocument = new BsonDocument()

      def apply(key: String, value: BsonValue) = new BsonDocument(key, value)

      def apply(elems: Iterable[(String, BsonValue)]): BsonDocument = {
         val bsonDoc = new BsonDocument()
         elems.foreach(e => bsonDoc.put(e._1, e._2))
         bsonDoc
      }

      def apply(elems: (String, BsonValue)*): BsonDocument = {
         val bsonDoc = new BsonDocument()
         elems.foreach(e => bsonDoc.put(e._1, e._2))
         bsonDoc
      }
   }
}
