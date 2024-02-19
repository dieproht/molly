package molly.core

import org.bson.BsonDocument
import org.bson.conversions.Bson

/** Convenience layer over
  * [[https://mongodb.github.io/mongo-java-driver/4.11/apidocs/mongodb-driver-core/com/mongodb/client/model/package-summary.html the Java driver's model classes]].
  */
object model {

   type BulkWriteOptions = com.mongodb.client.model.BulkWriteOptions

   object BulkWriteOptions {
      def apply(): BulkWriteOptions = new BulkWriteOptions()
   }

   type CreateIndexOptions = com.mongodb.client.model.CreateIndexOptions

   object CreateIndexOptions {
      def apply(): CreateIndexOptions = new CreateIndexOptions()
   }

   type DeleteManyModel = com.mongodb.client.model.DeleteManyModel[BsonDocument]

   object DeleteManyModel {
      def apply(filter: Bson): DeleteManyModel = new DeleteManyModel(filter)
      def apply(filter: Bson, options: DeleteOptions): DeleteManyModel = new DeleteManyModel(filter, options)
   }

   type DeleteOneModel = com.mongodb.client.model.DeleteOneModel[BsonDocument]

   object DeleteOneModel {
      def apply(filter: Bson): DeleteOneModel = new DeleteOneModel(filter)
      def apply(filter: Bson, options: DeleteOptions): DeleteOneModel = new DeleteOneModel(filter, options)
   }

   type DeleteOptions = com.mongodb.client.model.DeleteOptions

   object DeleteOptions {
      def apply(): DeleteOptions = new DeleteOptions()
   }

   type FindOneAndReplaceOptions = com.mongodb.client.model.FindOneAndReplaceOptions

   object FindOneAndReplaceOptions {
      def apply(): FindOneAndReplaceOptions = new FindOneAndReplaceOptions()
   }

   type FindOneAndUpdateOptions = com.mongodb.client.model.FindOneAndUpdateOptions

   object FindOneAndUpdateOptions {
      def apply(): FindOneAndUpdateOptions = new FindOneAndUpdateOptions()
   }

   type IndexModel = com.mongodb.client.model.IndexModel

   object IndexModel {
      def apply(keys: Bson): IndexModel = new IndexModel(keys)
      def apply(keys: Bson, options: IndexOptions): IndexModel = new IndexModel(keys, options)
   }

   type IndexOptions = com.mongodb.client.model.IndexOptions

   object IndexOptions {
      def apply(): IndexOptions = new IndexOptions()
   }

   type InsertOneModel = com.mongodb.client.model.InsertOneModel[BsonDocument]

   object InsertOneModel {
      def apply(document: BsonDocument): InsertOneModel = new InsertOneModel(document)
   }

   type ReplaceOneModel = com.mongodb.client.model.ReplaceOneModel[BsonDocument]

   object ReplaceOneModel {
      def apply(filter: Bson, replacement: BsonDocument): ReplaceOneModel = new ReplaceOneModel(filter, replacement)
      def apply(filter: Bson, replacement: BsonDocument, options: ReplaceOptions): ReplaceOneModel =
         new ReplaceOneModel(filter, replacement, options)
   }

   type ReplaceOptions = com.mongodb.client.model.ReplaceOptions

   object ReplaceOptions {
      def apply(): ReplaceOptions = new ReplaceOptions()
   }

   type UpdateManyModel = com.mongodb.client.model.UpdateManyModel[BsonDocument]

   object UpdateManyModel {
      def apply(filter: Bson, update: Bson): UpdateManyModel = new UpdateManyModel(filter, update)
      def apply(filter: Bson, update: Bson, options: UpdateOptions): UpdateManyModel =
         new UpdateManyModel(filter, update, options)
   }

   type UpdateOneModel = com.mongodb.client.model.UpdateOneModel[BsonDocument]

   object UpdateOneModel {
      def apply(filter: Bson, update: Bson): UpdateOneModel = new UpdateOneModel(filter, update)
      def apply(filter: Bson, update: Bson, options: UpdateOptions): UpdateOneModel =
         new UpdateOneModel(filter, update, options)
   }

   type UpdateOptions = com.mongodb.client.model.UpdateOptions

   object UpdateOptions {
      def apply(): UpdateOptions = new UpdateOptions()
   }

   type WriteModel = com.mongodb.client.model.WriteModel[BsonDocument]
}
