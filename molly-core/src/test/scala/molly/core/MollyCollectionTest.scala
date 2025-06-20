package molly.core

import cats.effect.IO
import com.dimafeng.testcontainers.MongoDBContainer
import com.mongodb.MongoBulkWriteException
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.Projections
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import molly.core.syntax.bsondocument.BsonDocumentCollection
import molly.core.syntax.model.CountOptions
import molly.core.syntax.model.CreateIndexOptions
import molly.core.syntax.model.DeleteOneModel
import molly.core.syntax.model.FindOneAndReplaceOptions
import molly.core.syntax.model.FindOneAndUpdateOptions
import molly.core.syntax.model.IndexModel
import molly.core.syntax.model.IndexOptions
import molly.core.syntax.model.InsertOneModel
import molly.core.syntax.model.ReplaceOptions
import molly.core.syntax.model.UpdateOptions
import molly.core.syntax.model.WriteModel
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString
import org.testcontainers.utility.DockerImageName
import weaver.IOSuite

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*

object MollyCollectionTest extends IOSuite with TestContainerForAll[IO] with MollyTestSupport:

    override val containerDef: MongoDBContainer.Def = MongoDBContainer.Def(DockerImageName.parse("mongo:7.0"))

    test("aggregate: perform aggregation pipeline on empty collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("aggregate1")
                results <- coll.aggregate(Seq(Aggregates.project(Projections.include("foo", "bar")))).list()
            yield expect(results.isEmpty)

    test("aggregate: perform aggregation pipeline on documents"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1))
                .append("foo", new BsonInt32(47))
                .append("bar", new BsonString("wer"))
                .append("bee", new BsonInt32(7576))
            val doc2 = new BsonDocument("_id", new BsonInt32(2))
                .append("foo", new BsonInt32(20))
                .append("bar", new BsonString("drg"))
                .append("bee", new BsonInt32(9438))
            val doc3 = new BsonDocument("_id", new BsonInt32(3))
                .append("foo", new BsonInt32(99))
                .append("bar", new BsonString("jrn"))
                .append("bee", new BsonInt32(3333))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("aggregate2")
                _       <- coll.insertMany(Seq(doc1, doc2, doc3))
                results <- coll.aggregate(Seq(Aggregates.project(Projections.include("foo", "bar")))).list()
            yield expect(results.size == 3)
                .and(expect(results.forall(_.containsKey("_id"))))
                .and(expect(results.forall(_.containsKey("foo"))))
                .and(expect(results.forall(_.containsKey("bar"))))
                .and(expect(results.forall(!_.containsKey("bee"))))

    test("bulkWrite: write given documents to collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("fooo", new BsonString("baz"))
            val doc3 = new BsonDocument("fu", new BsonInt32(24))
            val writeCommands = Seq(InsertOneModel(doc1), InsertOneModel(doc2), InsertOneModel(doc3))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("bulkWrite1")
                _       <- coll.bulkWrite(writeCommands)
                results <- coll.find().list()
            yield expect(results.size == 3)
                .and(expect(results.contains(doc1)))
                .and(expect(results.contains(doc2)))
                .and(expect(results.contains(doc3)))

    test("bulkWrite: execute different commands"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("foo", new BsonString("baz"))
            val writeCommands: Seq[WriteModel] =
                Seq(
                  InsertOneModel(doc1),
                  InsertOneModel(doc2),
                  DeleteOneModel(Filters.eq("foo", "bar"))
                )
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("bulkWrite2")
                _       <- coll.bulkWrite(writeCommands)
                results <- coll.find().list()
            yield expect(results.size == 1).and(expect(results.contains(doc2)))

    test("countDocuments: count documents given a filter"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("foo", new BsonString("baz"))
            val doc3 = new BsonDocument("foo", new BsonString("barry"))
            for
                db    <- client.getDatabase("test")
                coll  <- db.getCollection("countDocuments")
                _     <- coll.insertMany(Seq(doc1, doc2, doc3))
                count <- coll.countDocuments(Filters.regex("foo", "bar.*"))
            yield expect(count == 2L)

    test("countDocuments: count documents given a filter and options"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("foo", new BsonString("baz"))
            val doc3 = new BsonDocument("foo", new BsonString("barry"))
            for
                db    <- client.getDatabase("test")
                coll  <- db.getCollection("countDocuments2")
                _     <- coll.insertMany(Seq(doc1, doc2, doc3))
                count <- coll.countDocuments(Filters.regex("foo", "bar.*"), CountOptions().limit(1))
            yield expect(count == 1L)

    test("create and list index"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            for
                db        <- client.getDatabase("test")
                coll      <- db.getCollection("index1")
                idxCreate <- coll.createIndex(Indexes.ascending("foo"))
                idxList   <- coll.listIndexes()
            yield expect(idxCreate.contains("foo_1"))
                .and(expect(idxList.exists(i => i.getString("name") == "foo_1")))

    test("create and list index with options"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            for
                db        <- client.getDatabase("test")
                coll      <- db.getCollection("index2")
                idxCreate <- coll.createIndex(Indexes.ascending("foo"), IndexOptions().unique(true))
                idxList   <- coll.listIndexes()
            yield expect(idxCreate.contains("foo_1"))
                .and(expect(idxList.exists(i => i.getString("name") == "foo_1" && i.getBoolean("unique") == true)))

    test("create and list indexes"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val idx1 = IndexModel(Indexes.ascending("foo"))
            val idx2 = IndexModel(Indexes.descending("bar"))
            for
                db        <- client.getDatabase("test")
                coll      <- db.getCollection("indexes1")
                idxCreate <- coll.createIndexes(Seq(idx1, idx2))
                idxList   <- coll.listIndexes()
            yield expect(idxCreate.contains("foo_1"))
                .and(expect(idxCreate.contains("bar_-1")))
                .and(expect(idxList.exists(_.getString("name") == "foo_1")))
                .and(expect(idxList.exists(_.getString("name") == "bar_-1")))

    test("create and list indexes with options"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val idx1 = IndexModel(Indexes.ascending("foo"))
            val idx2 = IndexModel(Indexes.descending("bar"))
            for
                db        <- client.getDatabase("test")
                coll      <- db.getCollection("indexes2")
                idxCreate <- coll.createIndexes(Seq(idx1, idx2), CreateIndexOptions().maxTime(3, TimeUnit.SECONDS))
                idxList   <- coll.listIndexes()
            yield expect(idxCreate.contains("foo_1"))
                .and(expect(idxCreate.contains("bar_-1")))
                .and(expect(idxList.exists(_.getString("name") == "foo_1")))
                .and(expect(idxList.exists(_.getString("name") == "bar_-1")))

    test("deleteMany: delete given documents from collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("foo", new BsonString("baz"))
            val doc3 = new BsonDocument("foo", new BsonInt32(24))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("deleteMany")
                _       <- coll.insertMany(Seq(doc1, doc2, doc3))
                _       <- coll.deleteMany(Filters.in("foo", "bar", "baz"))
                results <- coll.find().list()
            yield expect(results.size == 1)
                .and(expect(results.contains(doc3)))

    test("deleteOne: delete one document from collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("foo", new BsonString("baz"))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("deleteOne")
                _       <- coll.insertMany(Seq(doc1, doc2))
                _       <- coll.deleteOne(Filters.eq("foo", "bar"))
                results <- coll.find().list()
            yield expect(results.size == 1)
                .and(expect(results.contains(doc2)))

    test("drop: remove collection from database"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc = new BsonDocument("foo", new BsonString("bar"))
            for
                db        <- client.getDatabase("test-drop")
                collA     <- db.getCollection("dropA")
                collB     <- db.getCollection("dropB")
                _         <- collA.insertOne(doc)
                _         <- collB.insertOne(doc)
                _         <- collA.drop()
                collNames <- db.listCollectionNames()
            yield expect(collNames.size == 1)
                .and(expect(collNames.contains("dropB")))
                .and(expect(!collNames.contains("dropA")))

    test("estimatedDocumentCount: estimate document count"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("foo", new BsonString("baz"))
            val doc3 = new BsonDocument("foo", new BsonString("barry"))
            for
                db    <- client.getDatabase("test")
                coll  <- db.getCollection("estimatedDocumentCount")
                _     <- coll.insertMany(Seq(doc1, doc2, doc3))
                count <- coll.estimatedDocumentCount()
            yield expect(count == 3L)

    test("find: return no document from empty collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("find1")
                results <- coll.find().list()
            yield expect(results.isEmpty)

    test("find: return all documents from collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("fooo", new BsonString("baz"))
            val doc3 = new BsonDocument("fu", new BsonInt32(24))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("find2")
                _       <- coll.insertMany(Seq(doc1, doc2, doc3))
                results <- coll.find().list()
            yield expect(results.size == 3)
                .and(expect(results.contains(doc1)))
                .and(expect(results.contains(doc2)))
                .and(expect(results.contains(doc3)))

    test("find first: return first found document from collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("fooo", new BsonString("baz"))
            val doc3 = new BsonDocument("fu", new BsonInt32(24))
            for
                db     <- client.getDatabase("test")
                coll   <- db.getCollection("find3")
                _      <- coll.insertMany(Seq(doc1, doc2, doc3))
                result <- coll.find().first()
            yield expect(result.isDefined)
                .and(
                  expect(result.contains(doc1))
                      .or(expect(result.contains(doc2)))
                      .or(expect(result.contains(doc3)))
                )

    test("find first: return no document when there is no match"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("foo", new BsonString("baz"))
            val doc3 = new BsonDocument("foo", new BsonString("lol"))
            for
                db     <- client.getDatabase("test")
                coll   <- db.getCollection("find4")
                _      <- coll.insertMany(Seq(doc1, doc2, doc3))
                result <- coll.find(Filters.eq("foo", "x")).first()
            yield expect(result.isEmpty)

    test("find: return all documents matching the given filter"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
            val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("find5")
                _       <- coll.insertMany(Seq(doc1, doc2, doc3))
                results <- coll.find(Filters.gt("x", 25)).list()
            yield expect(results.size == 2)
                .and(expect(results.contains(doc1)))
                .and(expect(results.contains(doc3)))

    test("find: return all documents matching the given chained filter"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
            val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("find6")
                _       <- coll.insertMany(Seq(doc1, doc2, doc3))
                results <- coll.find().filter(Filters.gt("x", 25)).list()
            yield expect(results.size == 2)
                .and(expect(results.contains(doc1)))
                .and(expect(results.contains(doc3)))

    test("findOneAndDelete: return on document and delete it from collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("baz"))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("findOneAndDelete1")
                _       <- coll.insertMany(Seq(doc1, doc2))
                resDoc  <- coll.findOneAndDelete(Filters.eq("_id", 2))
                resColl <- coll.find().list()
            yield expect(resDoc == Some(doc2))
                .and(expect(resColl.size == 1))
                .and(expect(resColl.contains(doc1)))

    test("findOneAndDelete: return and delete nothing if nothing matches the given filter"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("baz"))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("findOneAndDelete2")
                _       <- coll.insertMany(Seq(doc1, doc2))
                resDoc  <- coll.findOneAndDelete(Filters.eq("_id", 3))
                resColl <- coll.find().list()
            yield expect(resDoc == None)
                .and(expect(resColl.size == 2))
                .and(expect(resColl.contains(doc1)))
                .and(expect(resColl.contains(doc2)))

    test("findOneAndReplace: return one document and replace it in collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("baz"))
            val doc2a = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("yoo"))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("findOneAndReplace")
                _       <- coll.insertMany(Seq(doc1, doc2))
                resDoc  <- coll.findOneAndReplace(Filters.eq("_id", 2), doc2a)
                resColl <- coll.find().list()
            yield expect(resDoc == Some(doc2))
                .and(expect(resColl.size == 2))
                .and(expect(resColl.contains(doc1)))
                .and(expect(resColl.contains(doc2a)))

    test("findOneAndReplace: return and replace nothing if nothing matches the given filter"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("baz"))
            val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("foo", new BsonString("yoo"))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("findOneAndReplace1")
                _       <- coll.insertMany(Seq(doc1, doc2))
                resDoc  <- coll.findOneAndReplace(Filters.eq("_id", 3), doc3)
                resColl <- coll.find().list()
            yield expect(resDoc == None)
                .and(expect(resColl.size == 2))
                .and(expect(resColl.contains(doc1)))
                .and(expect(resColl.contains(doc2)))
                .and(expect(!resColl.contains(doc3)))

    test("findOneAndReplace: return one document and replace it in collection - insert if it doesn't exist"):
        containers =>
            withClient(containers): (client: MollyClient[IO]) =>
                val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("foo", new BsonString("bar"))
                val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("yoo"))
                for
                    db     <- client.getDatabase("test")
                    coll   <- db.getCollection("findOneAndReplace2")
                    _      <- coll.insertOne(doc1)
                    resDoc <- coll.findOneAndReplace(
                      Filters.eq("_id", 2),
                      doc2,
                      FindOneAndReplaceOptions().upsert(true)
                    )
                    resColl <- coll.find().list()
                yield expect(resDoc == None)
                    .and(expect(resColl.size == 2))
                    .and(expect(resColl.contains(doc1)))
                    .and(expect(resColl.contains(doc2)))

    test("findOneAndReplace: return one document and replace it in collection - do not insert if it doesn't exist"):
        containers =>
            withClient(containers): (client: MollyClient[IO]) =>
                val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("foo", new BsonString("bar"))
                val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("yoo"))
                for
                    db     <- client.getDatabase("test")
                    coll   <- db.getCollection("findOneAndReplace3")
                    _      <- coll.insertOne(doc1)
                    resDoc <- coll.findOneAndReplace(
                      Filters.eq("_id", 2),
                      doc2,
                      FindOneAndReplaceOptions().upsert(false)
                    )
                    resColl <- coll.find().list()
                yield expect(resDoc == None)
                    .and(expect(resColl.size == 1))
                    .and(expect(resColl.contains(doc1)))
                    .and(expect(!resColl.contains(doc2)))

    test("findOneAndUpdate: return one document and update it in collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("baz"))
            val doc2a = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("yoo"))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("findOneAndUpdate1")
                _       <- coll.insertMany(Seq(doc1, doc2))
                resDoc  <- coll.findOneAndUpdate(Filters.eq("_id", 2), Updates.set("foo", "yoo"))
                resColl <- coll.find().list()
            yield expect(resDoc == Some(doc2))
                .and(expect(resColl.size == 2))
                .and(expect(resColl.contains(doc1)))
                .and(expect(resColl.contains(doc2a)))

    test("findOneAndUpdate: return and update nothing if nothing matches the given filter"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("baz"))
            val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("foo", new BsonString("yoo"))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("findOneAndUpdate2")
                _       <- coll.insertMany(Seq(doc1, doc2))
                resDoc  <- coll.findOneAndUpdate(Filters.eq("_id", 3), Updates.set("foo", "yoo"))
                resColl <- coll.find().list()
            yield expect(resDoc == None)
                .and(expect(resColl.size == 2))
                .and(expect(resColl.contains(doc1)))
                .and(expect(resColl.contains(doc2)))
                .and(expect(!resColl.contains(doc3)))

    test("findOneAndUpdate: update one document in collection and return after"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("baz"))
            val doc2a = new BsonDocument("_id", new BsonInt32(2)).append("foo", new BsonString("yoo"))
            for
                db     <- client.getDatabase("test")
                coll   <- db.getCollection("findOneAndUpdate3")
                _      <- coll.insertMany(Seq(doc1, doc2))
                resDoc <- coll.findOneAndUpdate(
                  Filters.eq("_id", 2),
                  Updates.set("foo", "yoo"),
                  FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
                )
                resColl <- coll.find().list()
            yield expect(resDoc == Some(doc2a))
                .and(expect(resColl.size == 2))
                .and(expect(resColl.contains(doc1)))
                .and(expect(resColl.contains(doc2a)))

    test("insertMany: write given documents to collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("fooo", new BsonString("baz"))
            val doc3 = new BsonDocument("fu", new BsonInt32(24))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("insertMany")
                _       <- coll.insertMany(Seq(doc1, doc2, doc3))
                results <- coll.find().list()
            yield expect(results.size == 3)
                .and(expect(results.contains(doc1)))
                .and(expect(results.contains(doc2)))
                .and(expect(results.contains(doc3)))

    test("insertOne: write one document to collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc = new BsonDocument("foo", new BsonString("bar"))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("insertOne")
                _       <- coll.insertOne(doc)
                results <- coll.find().list()
            yield expect(results.size == 1)
                .and(expect(results.contains(doc)))

    test("replaceOne: replace one document in collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
            val doc2a = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(99))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("replaceOne1")
                _       <- coll.insertMany(Seq(doc1, doc2))
                res     <- coll.replaceOne(Filters.eq("_id", 2), doc2a)
                results <- coll.find().list()
            yield expect(results.size == 2)
                .and(expect(results.contains(doc1)))
                .and(expect(!results.contains(doc2)))
                .and(expect(results.contains(doc2a)))

    test("replaceOne: replace one document in collection - insert if it doesn't exist"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(99))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("replaceOne2")
                _       <- coll.insertMany(Seq(doc1))
                res     <- coll.replaceOne(Filters.eq("_id", 2), doc2, ReplaceOptions().upsert(true))
                results <- coll.find().list()
            yield expect(results.size == 2)
                .and(expect(results.contains(doc1)))
                .and(expect(results.contains(doc2)))

    test("replaceOne: replace one document in collection - do not insert if it doesn't exist"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(99))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("replaceOne3")
                _       <- coll.insertMany(Seq(doc1))
                res     <- coll.replaceOne(Filters.eq("_id", 2), doc2, ReplaceOptions().upsert(false))
                results <- coll.find().list()
            yield expect(results.size == 1)
                .and(expect(results.contains(doc1)))
                .and(expect(!results.contains(doc2)))

    test("sort: sort returned documents"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("foo", new BsonString("bar"))
            val doc2 = new BsonDocument("foo", new BsonString("baz"))
            val doc3 = new BsonDocument("foo", new BsonString("bay"))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("sort")
                _       <- coll.insertMany(Seq(doc1, doc2, doc3))
                results <- coll.find().sort(Sorts.descending("foo")).list()
            yield expect(results.size == 3)
                .and(expect(results(0) == doc2))
                .and(expect(results(1) == doc3))
                .and(expect(results(2) == doc1))

    test("updateMany: update multiple documents in collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
            val doc2a = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(25))
            val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))
            val doc3a = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(104))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("updateMany")
                _       <- coll.insertMany(Seq(doc1, doc2, doc3))
                res     <- coll.updateMany(Filters.in("_id", 2, 3), Updates.inc("x", 5))
                results <- coll.find().list()
            yield expect(results.size == 3)
                .and(expect(results.contains(doc1)))
                .and(expect(!results.contains(doc2)))
                .and(expect(results.contains(doc2a)))
                .and(expect(!results.contains(doc3)))
                .and(expect(results.contains(doc3a)))

    test("updateOne: update one document in collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
            val doc2a = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(24))
            val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("updateOne")
                _       <- coll.insertMany(Seq(doc1, doc2, doc3))
                res     <- coll.updateOne(Filters.eq("_id", 2), Updates.inc("x", 4))
                results <- coll.find().list()
            yield expect(results.size == 3)
                .and(expect(results.contains(doc1)))
                .and(expect(!results.contains(doc2)))
                .and(expect(results.contains(doc2a)))
                .and(expect(results.contains(doc3)))

    test("updateOne: upsert one document in collection"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
            val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))
            val doc4 = new BsonDocument("_id", new BsonInt32(4)).append("x", new BsonInt32(1024))
            for
                db      <- client.getDatabase("test")
                coll    <- db.getCollection("updateOne2")
                _       <- coll.insertMany(Seq(doc1, doc2, doc3))
                _       <- coll.updateOne(Filters.eq("_id", 4), Updates.inc("x", 1024), UpdateOptions().upsert(true))
                results <- coll.find().list()
            yield expect(results.size == 4)
                .and(expect(results.contains(doc1)))
                .and(expect(results.contains(doc2)))
                .and(expect(results.contains(doc3)))
                .and(expect(results.contains(doc4)))

    private val eta = 200.millis

    test("watch: return one change per inserted document"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
            val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))

            def runChangeStream(coll: BsonDocumentCollection[IO]) =
                coll.watch().stream(bufferSize = 1).take(3).compile.toList

            def insert(coll: BsonDocumentCollection[IO]) = IO.sleep(eta) >> coll.insertMany(Seq(doc1, doc2, doc3))

            for
                db     <- client.getDatabase("test")
                coll   <- db.getCollection("watch1")
                csDocs <- runChangeStream(coll).both(insert(coll)).map(_._1)
            yield expect(csDocs.size == 3)
                .and(expect(csDocs.exists(_.getDocumentKey() == new BsonDocument("_id", new BsonInt32(1)))))
                .and(expect(csDocs.exists(_.getDocumentKey() == new BsonDocument("_id", new BsonInt32(2)))))
                .and(expect(csDocs.exists(_.getDocumentKey() == new BsonDocument("_id", new BsonInt32(3)))))
                .and(expect(csDocs.forall(_.getOperationTypeString() == "insert")))
                .and(expect(csDocs.forall(_.getFullDocument() != null)))

    test("watch: return different changes"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
            val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))

            def runChangeStream(coll: BsonDocumentCollection[IO]) =
                coll.watch().stream(bufferSize = 1).take(4).compile.toList

            def insertAndUpdate(coll: BsonDocumentCollection[IO]) =
                IO.sleep(eta) >> coll.insertMany(Seq(doc1, doc2, doc3)) >> coll.updateOne(
                  Filters.eq("_id", 2),
                  Updates.set("x", 23)
                )

            for
                db     <- client.getDatabase("test")
                coll   <- db.getCollection("watch2")
                csDocs <- runChangeStream(coll).both(insertAndUpdate(coll)).map(_._1)
            yield expect(csDocs.size == 4)
                .and(expect(csDocs.exists(_.getDocumentKey() == new BsonDocument("_id", new BsonInt32(1)))))
                .and(expect(csDocs.exists(_.getDocumentKey() == new BsonDocument("_id", new BsonInt32(2)))))
                .and(expect(csDocs.exists(_.getDocumentKey() == new BsonDocument("_id", new BsonInt32(3)))))
                .and(expect(csDocs.take(3).forall(_.getOperationTypeString() == "insert")))
                .and(expect(csDocs.last.getOperationTypeString() == "update"))

    test("watch: return one change per inserted document with aggregation applied"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
            val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))

            val pipe = Seq(Aggregates.project(Projections.exclude("fullDocument")))

            def runChangeStream(coll: BsonDocumentCollection[IO]) =
                coll.watch(pipe).stream(bufferSize = 1).take(3).compile.toList

            def insert(coll: BsonDocumentCollection[IO]) = IO.sleep(eta) >> coll.insertMany(Seq(doc1, doc2, doc3))

            for
                db     <- client.getDatabase("test")
                coll   <- db.getCollection("watch3")
                csDocs <- runChangeStream(coll).both(insert(coll)).map(_._1)
            yield expect(csDocs.size == 3).and(expect(csDocs.forall(_.getFullDocument() == null)))

    test("propagate errors from underlying driver"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
            val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
            val doc2a = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(99))
            for
                db   <- client.getDatabase("test")
                coll <- db.getCollection("properror")
                res  <- coll.insertMany(Seq(doc1, doc2, doc2a)).attempt
            yield expect(res.left.map(_.getClass) == Left(classOf[MongoBulkWriteException]))
