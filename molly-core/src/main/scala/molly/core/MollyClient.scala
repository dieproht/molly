package molly.core

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.functor.*
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.bulk.ClientBulkWriteOptions
import com.mongodb.client.model.bulk.ClientBulkWriteResult
import com.mongodb.client.model.bulk.ClientNamespacedWriteModel
import com.mongodb.connection.ClusterDescription
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import molly.core.reactivestreams.fromSinglePublisher

import scala.jdk.CollectionConverters.*

/** Molly's counterpart to
  * [[https://mongodb.github.io/mongo-java-driver/5.5/apidocs/driver-reactive-streams/com/mongodb/reactivestreams/client/MongoClient.html MongoClient]].
  */
final case class MollyClient[F[_]] private (private[core] val delegate: MongoClient)(using f: Async[F]):

    /** [[https://mongodb.github.io/mongo-java-driver/5.5/apidocs/driver-reactive-streams/com/mongodb/reactivestreams/client/MongoClient.html#getDatabase(java.lang.String)]]
      */
    def getDatabase(name: String): F[MollyDatabase[F]] =
        f.delay(delegate.getDatabase(name)).map(MollyDatabase(_))

    /** Like [[this.getDatabase]], but returns a
      * [[https://typelevel.org/cats-effect/api/3.x/cats/effect/kernel/Resource.html Resource]]
      */
    def getDatabaseAsResource(name: String): Resource[F, MollyDatabase[F]] =
        Resource.eval(getDatabase(name))

    /** [[https://mongodb.github.io/mongo-java-driver/5.5/apidocs/driver-reactive-streams/com/mongodb/reactivestreams/client/MongoClient.html#getClusterDescription()]]
      */
    def getClusterDescription(): ClusterDescription = delegate.getClusterDescription()

    /** [[https://mongodb.github.io/mongo-java-driver/5.5/apidocs/driver-reactive-streams/com/mongodb/reactivestreams/client/MongoCluster.html#bulkWrite(java.util.List)]]
      */
    def bulkWrite(requests: Seq[ClientNamespacedWriteModel]): F[ClientBulkWriteResult] =
        fromSinglePublisher(delegate.bulkWrite(requests.asJava))

    /** [[https://mongodb.github.io/mongo-java-driver/5.5/apidocs/driver-reactive-streams/com/mongodb/reactivestreams/client/MongoCluster.html#bulkWrite(java.util.List,com.mongodb.client.model.bulk.ClientBulkWriteOptions)]]
      */
    def bulkWrite(
        requests: Seq[ClientNamespacedWriteModel],
        options: ClientBulkWriteOptions
    ): F[ClientBulkWriteResult] =
        fromSinglePublisher(delegate.bulkWrite(requests.asJava, options))

object MollyClient:

    def make[F[_]](clientSettings: MongoClientSettings)(using f: Async[F]): Resource[F, MollyClient[F]] =
        Resource
            .make(f.delay(MongoClients.create(clientSettings)))(c => f.delay(c.close()))
            .map(MollyClient(_))
