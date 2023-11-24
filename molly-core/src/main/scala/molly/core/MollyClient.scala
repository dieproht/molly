package molly.core

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.functor.*
import com.mongodb.MongoClientSettings
import com.mongodb.connection.ClusterDescription
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients

/** Molly's counterpart to
  * [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoClient.html MongoClient]].
  */
final case class MollyClient[F[_]: Async] private (private[core] val delegate: MongoClient) {

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoClient.html#getDatabase(java.lang.String)]]
     */
   def getDatabase(name: String): F[MollyDatabase[F]] =
      Async[F].delay(delegate.getDatabase(name)).map(MollyDatabase(_))

      /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoClient.html#getClusterDescription()]]
        */
   def getClusterDescription(): ClusterDescription = delegate.getClusterDescription()
}

object MollyClient {

   def make[F[_]: Async](clientSettings: MongoClientSettings): Resource[F, MollyClient[F]] =
      Resource
         .make(Async[F].delay(MongoClients.create(clientSettings)))(c => Async[F].delay(c.close()))
         .map(MollyClient(_))
}
