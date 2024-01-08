package molly.core

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.functor.*
import com.mongodb.reactivestreams.client.MongoDatabase
import org.bson.BsonDocument

import bsondocument.BsonDocumentCollection

/** Molly's counterpart to
  * [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoDatabase.html MongoDatabase]]
  *
  * @param delegate
  */
final case class MollyDatabase[F[_]: Async] private[core] (private[core] val delegate: MongoDatabase) {

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoDatabase.html#getCollection(java.lang.String)]]
     */
   def getCollection(collectionName: String): F[BsonDocumentCollection[F]] = {
      import bsondocument.bsonDocumentCodec
      Async[F].delay(delegate.getCollection(collectionName, classOf[BsonDocument])).map(MollyCollection(_))
   }

   /** Like [[this.getCollection]], but returns a
     * [[https://typelevel.org/cats-effect/api/3.x/cats/effect/kernel/Resource.html Resource]]
     */
   def getCollectionAsResource(collectionName: String): Resource[F, BsonDocumentCollection[F]] =
      Resource.eval(getCollection(collectionName))

   def getTypedCollection[A](collectionName: String)(implicit codec: MollyCodec[F, A]): F[MollyCollection[F, A]] =
      Async[F].delay(delegate.getCollection(collectionName, classOf[BsonDocument])).map(MollyCollection[F, A](_))

   def getTypedCollectionAsResource[A](collectionName: String)(implicit
    codec: MollyCodec[F, A]
   ): Resource[F, MollyCollection[F, A]] =
      Resource.eval(getTypedCollection(collectionName))
}
