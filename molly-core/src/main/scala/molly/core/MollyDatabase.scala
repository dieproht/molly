package molly.core

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.functor.*
import com.mongodb.reactivestreams.client.MongoDatabase
import org.bson.BsonDocument

/** Molly's counterpart to
  * [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoDatabase.html MongoDatabase]]
  *
  * @param delegate
  */
final case class MollyDatabase[F[_]: Async] private[core] (private[core] val delegate: MongoDatabase) {

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoDatabase.html#getCollection(java.lang.String)]]
     */
   def getCollection(collectionName: String): F[MollyCollection[F]] =
      Async[F].delay(delegate.getCollection(collectionName, classOf[BsonDocument])).map(MollyCollection(_))

   /** Like [[this.getCollection]], but returns a
     * [[https://typelevel.org/cats-effect/api/3.x/cats/effect/kernel/Resource.html Resource]]
     */
   def getCollectionAsResource(collectionName: String): Resource[F, MollyCollection[F]] =
      Resource.eval(getCollection(collectionName))
}
