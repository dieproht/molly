package molly.core

import cats.effect.kernel.Async
import com.mongodb.client.MongoCollection
import molly.core.query.SyncWatchQuery
import org.bson.BsonDocument
import org.bson.conversions.Bson

import scala.jdk.CollectionConverters.*

/** Molly's counterpart to
  * [[https://mongodb.github.io/mongo-java-driver/5.3/apidocs/mongodb-driver-sync/com/mongodb/client/MongoCollection.html MongoCollection]].
  */
final case class MollySyncCollection[F[_], A] private[core] (private[core] val delegate: MongoCollection[BsonDocument])(
    using
    Async[F],
    MollyCodec[F, A]
):

  /** [[https://mongodb.github.io/mongo-java-driver/5.3/apidocs/mongodb-driver-sync/com/mongodb/client/MongoCollection.html#watch()]]
    */
  def watch(): SyncWatchQuery[F, A] = SyncWatchQuery(delegate.watch(classOf[BsonDocument]))

  /** [[https://mongodb.github.io/mongo-java-driver/5.3/apidocs/mongodb-driver-sync/com/mongodb/client/MongoCollection.html#watch(java.util.List)]]
    */
  def watch(pipeline: Seq[Bson]): SyncWatchQuery[F, A] =
    SyncWatchQuery(delegate.watch(pipeline.asJava, classOf[BsonDocument]))
