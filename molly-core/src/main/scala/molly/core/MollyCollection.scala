package molly.core

import cats.effect.kernel.Async
import cats.syntax.functor.*
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.IndexModel
import com.mongodb.client.model.WriteModel
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertManyResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.reactivestreams.client.MongoCollection
import molly.core.query.FindQuery
import molly.core.query.WatchQuery
import molly.core.reactivestreams.fromPublisher
import molly.core.reactivestreams.fromSinglePublisher
import org.bson.BsonDocument
import org.bson.Document
import org.bson.conversions.Bson

import java.lang
import scala.jdk.CollectionConverters.*

/** Molly's counterpart to
  * [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html MongoCollection]].
  */
final case class MollyCollection[F[_]: Async] private[core] (
 private[core] val delegate: MongoCollection[BsonDocument]
) {

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#find()]]
     */
   def find(): FindQuery[F] = FindQuery(delegate.find())

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#find(org.bson.conversions.Bson)]]
     */
   def find(filter: Bson): FindQuery[F] = FindQuery(delegate.find(filter))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#bulkWrite(java.util.List)]]
     */
   def bulkWrite(requests: Seq[WriteModel[BsonDocument]]): F[BulkWriteResult] =
      fromSinglePublisher(delegate.bulkWrite(requests.asJava))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#watch()]]
     */
   def watch(): WatchQuery[F] = WatchQuery(delegate.watch(classOf[BsonDocument]))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#insertOne(TDocument)]]
     */
   def insertOne(document: BsonDocument): F[InsertOneResult] = fromSinglePublisher(delegate.insertOne(document))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#insertMany(java.util.List)]]
     */
   def insertMany(documents: Seq[BsonDocument]): F[InsertManyResult] = fromSinglePublisher(
      delegate.insertMany(documents.asJava)
   )

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#deleteOne(org.bson.conversions.Bson)]]
     */
   def deleteOne(filter: Bson): F[DeleteResult] = fromSinglePublisher(delegate.deleteOne(filter))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#deleteMany(org.bson.conversions.Bson)]]
     */
   def deleteMany(filter: Bson): F[DeleteResult] = fromSinglePublisher(delegate.deleteMany(filter))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#replaceOne(org.bson.conversions.Bson,TDocument)]]
     */
   def replaceOne(filter: Bson, replacement: BsonDocument): F[UpdateResult] = fromSinglePublisher(
      delegate.replaceOne(filter, replacement)
   )

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#updateOne(org.bson.conversions.Bson,org.bson.conversions.Bson)]]
     */
   def updateOne(filter: Bson, update: Bson): F[UpdateResult] = fromSinglePublisher(delegate.updateOne(filter, update))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#updateMany(org.bson.conversions.Bson,org.bson.conversions.Bson)]]
     */
   def updateMany(filter: Bson, update: Bson): F[UpdateResult] = fromSinglePublisher(
      delegate.updateMany(filter, update)
   )

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#findOneAndDelete(org.bson.conversions.Bson)]]
     */
   def findOneAndDelete(filter: Bson): F[BsonDocument] = fromSinglePublisher(delegate.findOneAndDelete(filter))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#findOneAndReplace(org.bson.conversions.Bson,TDocument)]]
     */
   def findOneAndReplace(filter: Bson, replacement: BsonDocument): F[BsonDocument] = fromSinglePublisher(
      delegate.findOneAndReplace(filter, replacement)
   )

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#findOneAndUpdate(com.mongodb.reactivestreams.client.ClientSession,org.bson.conversions.Bson,org.bson.conversions.Bson)]]
     */
   def findOneAndUpdate(filter: Bson, update: Bson): F[BsonDocument] = fromSinglePublisher(
      delegate.findOneAndUpdate(filter, update)
   )

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#createIndexes(java.util.List)]]
     */
   def createIndexes(indexes: Seq[IndexModel]): F[List[String]] =
      fromPublisher(delegate.createIndexes(indexes.asJava), 1).compile.toList

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#listIndexes()]]
     */
   def listIndexes(): F[List[Document]] = fromPublisher(delegate.listIndexes(), 1).compile.toList

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#countDocuments(org.bson.conversions.Bson)]]
     */
   def countDocuments(filter: Bson): F[Long] = fromSinglePublisher(delegate.countDocuments(filter)).map(_.toLong)
}
