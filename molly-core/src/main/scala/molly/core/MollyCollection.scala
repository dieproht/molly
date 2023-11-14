package molly.core

import cats.effect.kernel.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertManyResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.reactivestreams.client.MongoCollection
import molly.core.model.CreateIndexOptions
import molly.core.model.FindOneAndReplaceOptions
import molly.core.model.IndexModel
import molly.core.model.IndexOptions
import molly.core.model.ReplaceOptions
import molly.core.model.UpdateOptions
import molly.core.model.WriteModel
import molly.core.query.AggregateQuery
import molly.core.query.FindQuery
import molly.core.query.WatchQuery
import molly.core.reactivestreams.fromOptionPublisher
import molly.core.reactivestreams.fromSinglePublisher
import molly.core.reactivestreams.fromStreamPublisher
import org.bson.BsonDocument
import org.bson.Document
import org.bson.conversions.Bson

import java.lang
import scala.jdk.CollectionConverters.*

/** Molly's counterpart to
  * [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html MongoCollection]].
  */
final case class MollyCollection[F[_], A] private[core] (private[core] val delegate: MongoCollection[BsonDocument])(
 implicit
 f: Async[F],
 codec: MollyCodec[F, A]
) {

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#aggregate(java.util.List)]]
     */
   def aggregate(pipeline: Seq[Bson]): AggregateQuery[F] =
      AggregateQuery(delegate.aggregate(pipeline.asJava, classOf[BsonDocument]))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#bulkWrite(java.util.List)]]
     */
   def bulkWrite(requests: Seq[WriteModel]): F[BulkWriteResult] =
      fromSinglePublisher(delegate.bulkWrite(requests.asJava))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#countDocuments(org.bson.conversions.Bson)]]
     */
   def countDocuments(filter: Bson): F[Long] = fromSinglePublisher(delegate.countDocuments(filter)).map(_.toLong)

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#createIndex(org.bson.conversions.Bson)]]
     */
   def createIndex(key: Bson): F[String] = fromSinglePublisher(delegate.createIndex(key))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#createIndex(org.bson.conversions.Bson,com.mongodb.client.model.IndexOptions)]]
     */
   def createIndex(key: Bson, options: IndexOptions): F[String] =
      fromSinglePublisher(delegate.createIndex(key, options))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#createIndexes(java.util.List)]]
     */
   def createIndexes(indexes: Seq[IndexModel]): F[List[String]] =
      fromStreamPublisher(delegate.createIndexes(indexes.asJava), 1).compile.toList

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#createIndexes(java.util.List,com.mongodb.client.model.CreateIndexOptions)]]
     */
   def createIndexes(indexes: Seq[IndexModel], createIndexOptions: CreateIndexOptions): F[List[String]] =
      fromStreamPublisher(delegate.createIndexes(indexes.asJava, createIndexOptions), 1).compile.toList

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#deleteMany(org.bson.conversions.Bson)]]
     */
   def deleteMany(filter: Bson): F[DeleteResult] = fromSinglePublisher(delegate.deleteMany(filter))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#deleteOne(org.bson.conversions.Bson)]]
     */
   def deleteOne(filter: Bson): F[DeleteResult] = fromSinglePublisher(delegate.deleteOne(filter))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#estimatedDocumentCount()]]
     */
   def estimatedDocumentCount(): F[Long] = fromSinglePublisher(delegate.estimatedDocumentCount()).map(_.toLong)

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#find()]]
     */
   def find(): FindQuery[F, A] = FindQuery(delegate.find())

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#find(org.bson.conversions.Bson)]]
     */
   def find(filter: Bson): FindQuery[F, A] = FindQuery(delegate.find(filter))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#findOneAndDelete(org.bson.conversions.Bson)]]
     */
   def findOneAndDelete(filter: Bson): F[Option[A]] =
      for {
         resultDoc <- fromOptionPublisher(delegate.findOneAndDelete(filter))
         result    <- resultDoc.traverse(codec.decode)
      } yield result

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#findOneAndReplace(org.bson.conversions.Bson,TDocument)]]
     */
   def findOneAndReplace(filter: Bson, replacement: A): F[Option[A]] =
      for {
         replacementDoc <- codec.encode(replacement)
         resultDoc      <- fromOptionPublisher(delegate.findOneAndReplace(filter, replacementDoc))
         result         <- resultDoc.traverse(codec.decode)
      } yield result

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#findOneAndReplace(org.bson.conversions.Bson,TDocument,com.mongodb.client.model.FindOneAndReplaceOptions)]]
     */
   def findOneAndReplace(filter: Bson, replacement: A, options: FindOneAndReplaceOptions): F[Option[A]] =
      for {
         replacementDoc <- codec.encode(replacement)
         resultDoc      <- fromOptionPublisher(delegate.findOneAndReplace(filter, replacementDoc, options))
         result         <- resultDoc.traverse(codec.decode)
      } yield result

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#findOneAndUpdate(com.mongodb.reactivestreams.client.ClientSession,org.bson.conversions.Bson,org.bson.conversions.Bson)]]
     */
   def findOneAndUpdate(filter: Bson, update: Bson): F[Option[A]] =
      for {
         resultDoc <- fromOptionPublisher(delegate.findOneAndUpdate(filter, update))
         result    <- resultDoc.traverse(codec.decode)
      } yield result

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#insertMany(java.util.List)]]
     */
   def insertMany(documents: Seq[A]): F[InsertManyResult] =
      for {
         documumentsDocs <- documents.traverse(codec.encode)
         result          <- fromSinglePublisher(delegate.insertMany(documumentsDocs.asJava))
      } yield result

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#insertOne(TDocument)]]
     */
   def insertOne(document: A): F[InsertOneResult] =
      for {
         documentDoc <- codec.encode(document)
         result      <- fromSinglePublisher(delegate.insertOne(documentDoc))
      } yield result

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#listIndexes()]]
     */
   def listIndexes(): F[List[Document]] = fromStreamPublisher(delegate.listIndexes(), 1).compile.toList

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#replaceOne(org.bson.conversions.Bson,TDocument)]]
     */
   def replaceOne(filter: Bson, replacement: A): F[UpdateResult] =
      for {
         replacementDoc <- codec.encode(replacement)
         result         <- fromSinglePublisher(delegate.replaceOne(filter, replacementDoc))
      } yield result

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#replaceOne(org.bson.conversions.Bson,TDocument,com.mongodb.client.model.ReplaceOptions)]]
     */
   def replaceOne(filter: Bson, replacement: A, options: ReplaceOptions): F[UpdateResult] =
      for {
         replacementDoc <- codec.encode(replacement)
         result         <- fromSinglePublisher(delegate.replaceOne(filter, replacementDoc, options))
      } yield result

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#updateMany(org.bson.conversions.Bson,org.bson.conversions.Bson)]]
     */
   def updateMany(filter: Bson, update: Bson): F[UpdateResult] =
      fromSinglePublisher(delegate.updateMany(filter, update))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#updateOne(org.bson.conversions.Bson,org.bson.conversions.Bson)]]
     */
   def updateOne(filter: Bson, update: Bson): F[UpdateResult] = fromSinglePublisher(delegate.updateOne(filter, update))

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#updateOne(org.bson.conversions.Bson,org.bson.conversions.Bson, com.mongodb.client.model.UpdateOptions)]]
     */
   def updateOne(filter: Bson, update: Bson, options: UpdateOptions): F[UpdateResult] = fromSinglePublisher(
      delegate.updateOne(filter, update, options)
   )

   /** [[https://mongodb.github.io/mongo-java-driver/4.10/apidocs/mongodb-driver-reactivestreams/com/mongodb/reactivestreams/client/MongoCollection.html#watch()]]
     */
   def watch(): WatchQuery[F, A] = WatchQuery(delegate.watch(classOf[BsonDocument]))
}
