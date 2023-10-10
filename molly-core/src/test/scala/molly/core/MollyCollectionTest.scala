package molly.core

import cats.effect.IO
import com.dimafeng.testcontainers.MongoDBContainer
import com.mongodb.MongoBulkWriteException
import com.mongodb.client.model.CreateIndexOptions
import com.mongodb.client.model.DeleteOneModel
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.client.model.IndexModel
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.InsertOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates
import com.mongodb.client.model.WriteModel
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString
import org.testcontainers.utility.DockerImageName
import weaver.IOSuite

import java.util.concurrent.TimeUnit

object MollyCollectionTest extends IOSuite with TestContainerForAll[IO] with MollyTestSupport {

   override val containerDef: MongoDBContainer.Def = MongoDBContainer.Def(DockerImageName.parse("mongo:latest"))

   test("find: return no document from empty collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("find1")
            results <- coll.find().list
         } yield expect(results.isEmpty)
      }
   }

   test("find: return all documents from collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("foo", new BsonString("bar"))
         val doc2 = new BsonDocument("fooo", new BsonString("baz"))
         val doc3 = new BsonDocument("fu", new BsonInt32(24))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("find2")
            _       <- coll.insertMany(Seq(doc1, doc2, doc3))
            results <- coll.find().list
         } yield expect(results.size == 3)
            .and(expect(results.contains(doc1)))
            .and(expect(results.contains(doc2)))
            .and(expect(results.contains(doc3)))
      }
   }

   test("find first: return first found document from collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("foo", new BsonString("bar"))
         val doc2 = new BsonDocument("fooo", new BsonString("baz"))
         val doc3 = new BsonDocument("fu", new BsonInt32(24))
         for {
            db     <- client.getDatabase("test")
            coll   <- db.getCollection("find3")
            _      <- coll.insertMany(Seq(doc1, doc2, doc3))
            result <- coll.find().first
         } yield expect(result.isDefined)
            .and(
               expect(result.contains(doc1))
                  .or(expect(result.contains(doc2)))
                  .or(expect(result.contains(doc3)))
            )
      }
   }

   test("find first: return no document when there is no match") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("foo", new BsonString("bar"))
         val doc2 = new BsonDocument("foo", new BsonString("baz"))
         val doc3 = new BsonDocument("foo", new BsonString("lol"))
         for {
            db     <- client.getDatabase("test")
            coll   <- db.getCollection("find4")
            _      <- coll.insertMany(Seq(doc1, doc2, doc3))
            result <- coll.find(Filters.eq("foo", "x")).first
         } yield expect(result.isEmpty)
      }
   }

   test("find: return all documents matching the given filter") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
         val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
         val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("find5")
            _       <- coll.insertMany(Seq(doc1, doc2, doc3))
            results <- coll.find(Filters.gt("x", 25)).list
         } yield expect(results.size == 2)
            .and(expect(results.contains(doc1)))
            .and(expect(results.contains(doc3)))
      }
   }

   test("find: return all documents matching the given chained filter") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
         val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
         val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("find6")
            _       <- coll.insertMany(Seq(doc1, doc2, doc3))
            results <- coll.find().filter(Filters.gt("x", 25)).list
         } yield expect(results.size == 2)
            .and(expect(results.contains(doc1)))
            .and(expect(results.contains(doc3)))
      }
   }

   test("bulkWrite: write given documents to collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("foo", new BsonString("bar"))
         val doc2 = new BsonDocument("fooo", new BsonString("baz"))
         val doc3 = new BsonDocument("fu", new BsonInt32(24))
         val writeCommands = Seq(new InsertOneModel(doc1), new InsertOneModel(doc2), new InsertOneModel(doc3))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("bulkWrite1")
            _       <- coll.bulkWrite(writeCommands)
            results <- coll.find().list
         } yield expect(results.size == 3)
            .and(expect(results.contains(doc1)))
            .and(expect(results.contains(doc2)))
            .and(expect(results.contains(doc3)))
      }
   }

   test("bulkWrite: execute different commands") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("foo", new BsonString("bar"))
         val doc2 = new BsonDocument("foo", new BsonString("baz"))
         val writeCommands: Seq[WriteModel[BsonDocument]] =
            Seq(
               new InsertOneModel(doc1),
               new InsertOneModel(doc2),
               new DeleteOneModel(Filters.eq("foo", "bar"))
            )
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("bulkWrite2")
            _       <- coll.bulkWrite(writeCommands)
            results <- coll.find().list
         } yield expect(results.size == 1).and(expect(results.contains(doc2)))
      }
   }

   test("watch: return one change per inserted document") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
         val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
         val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))
         def runChangeStream(coll: MollyCollection[IO]) = coll.watch().stream.take(3).compile.toList
         for {
            db     <- client.getDatabase("test")
            coll   <- db.getCollection("watch1")
            csDocs <- runChangeStream(coll).both(coll.insertMany(Seq(doc1, doc2, doc3))).map(_._1)
         } yield expect(csDocs.size == 3)
            .and(expect(csDocs.exists(_.getDocumentKey() == new BsonDocument("_id", new BsonInt32(1)))))
            .and(expect(csDocs.exists(_.getDocumentKey() == new BsonDocument("_id", new BsonInt32(2)))))
            .and(expect(csDocs.exists(_.getDocumentKey() == new BsonDocument("_id", new BsonInt32(3)))))
            .and(expect(csDocs.forall(_.getOperationTypeString() == "insert")))
      }
   }

   test("watch: return different changes") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
         val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
         val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))
         def runChangeStream(coll: MollyCollection[IO]) = coll.watch().stream.take(4).compile.toList
         def insertAndUpdate(coll: MollyCollection[IO]) =
            coll.insertMany(Seq(doc1, doc2, doc3)) >> coll.updateOne(Filters.eq("_id", 2), Updates.set("x", 23))
         for {
            db     <- client.getDatabase("test")
            coll   <- db.getCollection("watch2")
            csDocs <- runChangeStream(coll).both(insertAndUpdate(coll)).map(_._1)
         } yield expect(csDocs.size == 4)
            .and(expect(csDocs.exists(_.getDocumentKey() == new BsonDocument("_id", new BsonInt32(1)))))
            .and(expect(csDocs.exists(_.getDocumentKey() == new BsonDocument("_id", new BsonInt32(2)))))
            .and(expect(csDocs.exists(_.getDocumentKey() == new BsonDocument("_id", new BsonInt32(3)))))
            .and(expect(csDocs.take(3).forall(_.getOperationTypeString() == "insert")))
            .and(expect(csDocs.last.getOperationTypeString() == "update"))
      }
   }

   test("insertOne: write one document to collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc = new BsonDocument("foo", new BsonString("bar"))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("insertOne")
            _       <- coll.insertOne(doc)
            results <- coll.find().list
         } yield expect(results.size == 1)
            .and(expect(results.contains(doc)))
      }
   }

   test("insertMany: write given documents to collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("foo", new BsonString("bar"))
         val doc2 = new BsonDocument("fooo", new BsonString("baz"))
         val doc3 = new BsonDocument("fu", new BsonInt32(24))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("insertMany")
            _       <- coll.insertMany(Seq(doc1, doc2, doc3))
            results <- coll.find().list
         } yield expect(results.size == 3)
            .and(expect(results.contains(doc1)))
            .and(expect(results.contains(doc2)))
            .and(expect(results.contains(doc3)))
      }
   }

   test("deleteOne: delete one document from collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("foo", new BsonString("bar"))
         val doc2 = new BsonDocument("foo", new BsonString("baz"))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("deleteOne")
            _       <- coll.insertMany(Seq(doc1, doc2))
            _       <- coll.deleteOne(Filters.eq("foo", "bar"))
            results <- coll.find().list
         } yield expect(results.size == 1)
            .and(expect(results.contains(doc2)))
      }
   }

   test("deleteMany: delete given documents from collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("foo", new BsonString("bar"))
         val doc2 = new BsonDocument("foo", new BsonString("baz"))
         val doc3 = new BsonDocument("foo", new BsonInt32(24))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("deleteMany")
            _       <- coll.insertMany(Seq(doc1, doc2, doc3))
            _       <- coll.deleteMany(Filters.in("foo", "bar", "baz"))
            results <- coll.find().list
         } yield expect(results.size == 1)
            .and(expect(results.contains(doc3)))
      }
   }

   test("replaceOne: replace one document in collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
         val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
         val doc2a = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(99))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("replaceOne1")
            _       <- coll.insertMany(Seq(doc1, doc2))
            res     <- coll.replaceOne(Filters.eq("_id", 2), doc2a)
            results <- coll.find().list
         } yield expect(results.size == 2)
            .and(expect(results.contains(doc1)))
            .and(expect(!results.contains(doc2)))
            .and(expect(results.contains(doc2a)))
      }
   }

   test("replaceOne: replace one document in collection - insert if it doesn't exist") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
         val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(99))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("replaceOne2")
            _       <- coll.insertMany(Seq(doc1))
            res     <- coll.replaceOne(Filters.eq("_id", 2), doc2, new ReplaceOptions().upsert(true))
            results <- coll.find().list
         } yield expect(results.size == 2)
            .and(expect(results.contains(doc1)))
            .and(expect(results.contains(doc2)))
      }
   }

   test("replaceOne: replace one document in collection - do not insert if it doesn't exist") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
         val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(99))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("replaceOne3")
            _       <- coll.insertMany(Seq(doc1))
            res     <- coll.replaceOne(Filters.eq("_id", 2), doc2, new ReplaceOptions().upsert(false))
            results <- coll.find().list
         } yield expect(results.size == 1)
            .and(expect(results.contains(doc1)))
            .and(expect(!results.contains(doc2)))
      }
   }

   test("updateOne: update one document in collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
         val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
         val doc2a = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(24))
         val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("updateOne")
            _       <- coll.insertMany(Seq(doc1, doc2, doc3))
            res     <- coll.updateOne(Filters.eq("_id", 2), Updates.inc("x", 4))
            results <- coll.find().list
         } yield expect(results.size == 3)
            .and(expect(results.contains(doc1)))
            .and(expect(!results.contains(doc2)))
            .and(expect(results.contains(doc2a)))
            .and(expect(results.contains(doc3)))
      }
   }

   test("updateMany: update multiple documents in collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
         val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
         val doc2a = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(25))
         val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))
         val doc3a = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(104))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("updateMany")
            _       <- coll.insertMany(Seq(doc1, doc2, doc3))
            res     <- coll.updateMany(Filters.in("_id", 2, 3), Updates.inc("x", 5))
            results <- coll.find().list
         } yield expect(results.size == 3)
            .and(expect(results.contains(doc1)))
            .and(expect(!results.contains(doc2)))
            .and(expect(results.contains(doc2a)))
            .and(expect(!results.contains(doc3)))
            .and(expect(results.contains(doc3a)))
      }
   }

   test("findOneAndDelete: return on document and delete it from collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("foo", new BsonString("bar"))
         val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("baz"))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("findOneAndDelete")
            _       <- coll.insertMany(Seq(doc1, doc2))
            resDoc  <- coll.findOneAndDelete(Filters.eq("_id", 2))
            resColl <- coll.find().list
         } yield expect(resDoc == doc2)
            .and(expect(resColl.size == 1))
            .and(expect(resColl.contains(doc1)))
      }
   }

   test("findOneAndReplace: return one document and replace it in collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("foo", new BsonString("bar"))
         val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("baz"))
         val doc2a = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("yoo"))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("findOneAndReplace1")
            _       <- coll.insertMany(Seq(doc1, doc2))
            resDoc  <- coll.findOneAndReplace(Filters.eq("_id", 2), doc2a)
            resColl <- coll.find().list
         } yield expect(resDoc == doc2)
            .and(expect(resColl.size == 2))
            .and(expect(resColl.contains(doc1)))
            .and(expect(resColl.contains(doc2a)))
      }
   }

   test("findOneAndReplace: return one document and replace it in collection - insert if it doesn't exist") {
      containers =>
         withClient(containers) { (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("yoo"))
            for {
               db   <- client.getDatabase("test")
               coll <- db.getCollection("findOneAndReplace2")
               _    <- coll.insertOne(doc1)
               resDoc <- coll.findOneAndReplace(
                  Filters.eq("_id", 2),
                  doc2,
                  new FindOneAndReplaceOptions().upsert(true)
               )
               resColl <- coll.find().list
            } yield expect(resDoc == None)
               .and(expect(resColl.size == 2))
               .and(expect(resColl.contains(doc1)))
               .and(expect(resColl.contains(doc2)))
         }
   }

   test("findOneAndReplace: return one document and replace it in collection - do not insert if it doesn't exist") {
      containers =>
         withClient(containers) { (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("yoo"))
            for {
               db   <- client.getDatabase("test")
               coll <- db.getCollection("findOneAndReplace3")
               _    <- coll.insertOne(doc1)
               resDoc <- coll.findOneAndReplace(
                  Filters.eq("_id", 2),
                  doc2,
                  new FindOneAndReplaceOptions().upsert(false)
               )
               resColl <- coll.find().list
            } yield expect(resDoc == None)
               .and(expect(resColl.size == 1))
               .and(expect(resColl.contains(doc1)))
               .and(expect(!resColl.contains(doc2)))
         }
   }

   test("findOneAndUpdate: return on document and replace it in collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("foo", new BsonString("bar"))
         val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("baz"))
         val doc2a = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("yoo"))
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getCollection("findOneAndUpdate")
            _       <- coll.insertMany(Seq(doc1, doc2))
            resDoc  <- coll.findOneAndUpdate(Filters.eq("_id", 2), Updates.set("foo", "yoo"))
            resColl <- coll.find().list
         } yield expect(resDoc == doc2)
            .and(expect(resColl.size == 2))
            .and(expect(resColl.contains(doc1)))
            .and(expect(resColl.contains(doc2a)))
      }
   }

   test("propagate errors from underlying driver") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
         val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
         val doc2a = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(99))
         for {
            db   <- client.getDatabase("test")
            coll <- db.getCollection("properror")
            res  <- coll.insertMany(Seq(doc1, doc2, doc2a)).attempt
         } yield expect(res.left.map(_.getClass) == Left(classOf[MongoBulkWriteException]))
      }
   }

   test("create and list indexes") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val idx1 = new IndexModel(Indexes.ascending("foo"))
         val idx2 = new IndexModel(Indexes.descending("bar"))
         for {
            db        <- client.getDatabase("test")
            coll      <- db.getCollection("indexes1")
            idxCreate <- coll.createIndexes(Seq(idx1, idx2))
            idxList   <- coll.listIndexes()
         } yield {
            expect(idxCreate.contains("foo_1"))
               .and(expect(idxCreate.contains("bar_-1")))
               .and(expect(idxList.exists(_.getString("name") == "foo_1")))
               .and(expect(idxList.exists(_.getString("name") == "bar_-1")))
         }
      }
   }

   test("create and list indexes with options") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val idx1 = new IndexModel(Indexes.ascending("foo"))
         val idx2 = new IndexModel(Indexes.descending("bar"))
         for {
            db        <- client.getDatabase("test")
            coll      <- db.getCollection("indexes2")
            idxCreate <- coll.createIndexes(Seq(idx1, idx2), new CreateIndexOptions().maxTime(3, TimeUnit.SECONDS))
            idxList   <- coll.listIndexes()
         } yield {
            expect(idxCreate.contains("foo_1"))
               .and(expect(idxCreate.contains("bar_-1")))
               .and(expect(idxList.exists(_.getString("name") == "foo_1")))
               .and(expect(idxList.exists(_.getString("name") == "bar_-1")))
         }
      }
   }

   test("create and list index") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db        <- client.getDatabase("test")
            coll      <- db.getCollection("index1")
            idxCreate <- coll.createIndex(Indexes.ascending("foo"))
            idxList   <- coll.listIndexes()
         } yield {
            expect(idxCreate.contains("foo_1"))
               .and(expect(idxList.exists(i => i.getString("name") == "foo_1")))
         }
      }
   }

   test("create and list index with options") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db        <- client.getDatabase("test")
            coll      <- db.getCollection("index2")
            idxCreate <- coll.createIndex(Indexes.ascending("foo"), new IndexOptions().unique(true))
            idxList   <- coll.listIndexes()
         } yield {
            expect(idxCreate.contains("foo_1"))
               .and(expect(idxList.exists(i => i.getString("name") == "foo_1" && i.getBoolean("unique") == true)))
         }
      }
   }

   test("countDocuments: count documents given a filter") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("foo", new BsonString("bar"))
         val doc2 = new BsonDocument("foo", new BsonString("baz"))
         val doc3 = new BsonDocument("foo", new BsonString("barry"))
         for {
            db    <- client.getDatabase("test")
            coll  <- db.getCollection("countDocuments")
            _     <- coll.insertMany(Seq(doc1, doc2, doc3))
            count <- coll.countDocuments(Filters.regex("foo", "bar.*"))
         } yield expect(count == 2L)
      }
   }

   test("estimatedDocumentCount: estimate document count") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val doc1 = new BsonDocument("foo", new BsonString("bar"))
         val doc2 = new BsonDocument("foo", new BsonString("baz"))
         val doc3 = new BsonDocument("foo", new BsonString("barry"))
         for {
            db    <- client.getDatabase("test")
            coll  <- db.getCollection("estimatedDocumentCount")
            _     <- coll.insertMany(Seq(doc1, doc2, doc3))
            count <- coll.estimatedDocumentCount()
         } yield expect(count == 3L)
      }
   }

}
