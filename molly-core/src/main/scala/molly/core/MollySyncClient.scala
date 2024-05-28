package molly.core

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import cats.syntax.functor.*

/** Molly's counterpart to
  * [[https://mongodb.github.io/mongo-java-driver/5.1/apidocs/mongodb-driver-sync/com/mongodb/client/MongoClient.html MongoClient]].
  */
final case class MollySyncClient[F[_]: Async] private (private[core] val delegate: MongoClient) {

   /** [[https://mongodb.github.io/mongo-java-driver/5.1/apidocs/mongodb-driver-sync/com/mongodb/client/MongoClient.html#getDatabase(java.lang.String)]]
     */
   def getDatabase(name: String): F[MollySyncDatabase[F]] =
      Async[F].delay(delegate.getDatabase(name)).map(MollySyncDatabase(_))

   /** Like [[this.getDatabase]], but returns a
     * [[https://typelevel.org/cats-effect/api/3.x/cats/effect/kernel/Resource.html Resource]]
     */
   def getDatabaseAsResource(name: String): Resource[F, MollySyncDatabase[F]] =
      Resource.eval(getDatabase(name))

}

object MollySyncClient {

   def make[F[_]: Async](clientSettings: MongoClientSettings): Resource[F, MollySyncClient[F]] =
      Resource
         .make(Async[F].delay(MongoClients.create(clientSettings)))(c => Async[F].delay(c.close()))
         .map(MollySyncClient(_))
}
