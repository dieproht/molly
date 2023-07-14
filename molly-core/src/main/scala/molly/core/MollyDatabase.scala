package molly.core

import cats.effect.kernel.Async
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
}
