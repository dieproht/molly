package molly.core

import cats.effect.IO
import com.dimafeng.testcontainers.MongoDBContainer
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Updates
import molly.core.syntax.bsondocument.BsonDocumentCollection
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.testcontainers.utility.DockerImageName
import weaver.IOSuite

import java.util.Objects
import scala.concurrent.duration.*
object MollySyncCollectionTest extends IOSuite, TestContainerForAll[IO], MollyTestSupport:

    override val containerDef: MongoDBContainer.Def = MongoDBContainer.Def(DockerImageName.parse(mongoVersion))

    private val eta = 200.millis

    test("watch: return one change per inserted document"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            withSyncClient(containers): (syncClient: MollySyncClient[IO]) =>
                val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
                val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
                val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))

                def runChangeStream(coll: MollySyncCollection[IO, BsonDocument]) =
                    coll.watch().stream(bufferSize = 1).take(3).compile.toList

                def insert(coll: BsonDocumentCollection[IO]) = IO.sleep(eta) >> coll.insertMany(Seq(doc1, doc2, doc3))

                for
                    db       <- client.getDatabase("test")
                    syncDb   <- syncClient.getDatabase("test")
                    coll     <- db.getCollection("watch1")
                    syncColl <- syncDb.getCollection("watch1")
                    csDocs   <- runChangeStream(syncColl).both(insert(coll)).map(_._1)
                yield expect(csDocs.size == 3)
                    .and(expect(csDocs.exists(_.getDocumentKey == new BsonDocument("_id", new BsonInt32(1)))))
                    .and(expect(csDocs.exists(_.getDocumentKey == new BsonDocument("_id", new BsonInt32(2)))))
                    .and(expect(csDocs.exists(_.getDocumentKey == new BsonDocument("_id", new BsonInt32(3)))))
                    .and(expect(csDocs.forall(_.getOperationTypeString == "insert")))
                    .and(expect(csDocs.forall(d => Objects.nonNull(d.getFullDocument))))

    test("watch: return one change per inserted document with buffer size greater than result size"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            withSyncClient(containers): (syncClient: MollySyncClient[IO]) =>
                val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
                val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
                val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))

                def runChangeStream(coll: MollySyncCollection[IO, BsonDocument]) =
                    coll.watch().stream(bufferSize = 16, timeout = 3.seconds).take(3).compile.toList

                def insert(coll: BsonDocumentCollection[IO]) = IO.sleep(eta) >> coll.insertMany(Seq(doc1, doc2, doc3))

                for
                    db       <- client.getDatabase("test")
                    syncDb   <- syncClient.getDatabase("test")
                    coll     <- db.getCollection("watch2")
                    syncColl <- syncDb.getCollection("watch2")
                    csDocs   <- runChangeStream(syncColl).both(insert(coll)).map(_._1)
                yield expect(csDocs.size == 3)
                    .and(expect(csDocs.exists(_.getDocumentKey == new BsonDocument("_id", new BsonInt32(1)))))
                    .and(expect(csDocs.exists(_.getDocumentKey == new BsonDocument("_id", new BsonInt32(2)))))
                    .and(expect(csDocs.exists(_.getDocumentKey == new BsonDocument("_id", new BsonInt32(3)))))
                    .and(expect(csDocs.forall(_.getOperationTypeString == "insert")))

    test("watch: return different changes"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            withSyncClient(containers): (syncClient: MollySyncClient[IO]) =>
                val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
                val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
                val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))

                def runChangeStream(coll: MollySyncCollection[IO, BsonDocument]) =
                    coll.watch().stream(bufferSize = 1).take(4).compile.toList

                def insertAndUpdate(coll: BsonDocumentCollection[IO]) =
                    IO.sleep(eta) >> coll.insertMany(Seq(doc1, doc2, doc3)) >> coll.updateOne(
                      Filters.eq("_id", 2),
                      Updates.set("x", 23)
                    )

                for
                    db       <- client.getDatabase("test")
                    syncDb   <- syncClient.getDatabase("test")
                    coll     <- db.getCollection("watch3")
                    syncColl <- syncDb.getCollection("watch3")
                    csDocs   <- runChangeStream(syncColl).both(insertAndUpdate(coll)).map(_._1)
                yield expect(csDocs.size == 4)
                    .and(expect(csDocs.exists(_.getDocumentKey == new BsonDocument("_id", new BsonInt32(1)))))
                    .and(expect(csDocs.exists(_.getDocumentKey == new BsonDocument("_id", new BsonInt32(2)))))
                    .and(expect(csDocs.exists(_.getDocumentKey == new BsonDocument("_id", new BsonInt32(3)))))
                    .and(expect(csDocs.take(3).forall(_.getOperationTypeString == "insert")))
                    .and(expect(csDocs.last.getOperationTypeString == "update"))

    test("watch: return one change per inserted document with aggregation applied"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            withSyncClient(containers): (syncClient: MollySyncClient[IO]) =>
                val doc1 = new BsonDocument("_id", new BsonInt32(1)).append("x", new BsonInt32(47))
                val doc2 = new BsonDocument("_id", new BsonInt32(2)).append("x", new BsonInt32(20))
                val doc3 = new BsonDocument("_id", new BsonInt32(3)).append("x", new BsonInt32(99))

                val pipe = Seq(Aggregates.project(Projections.exclude("fullDocument")))

                def runChangeStream(coll: MollySyncCollection[IO, BsonDocument]) =
                    coll.watch(pipe).stream(bufferSize = 1).take(3).compile.toList

                def insert(coll: BsonDocumentCollection[IO]) = IO.sleep(eta) >> coll.insertMany(Seq(doc1, doc2, doc3))

                for
                    db       <- client.getDatabase("test")
                    syncDb   <- syncClient.getDatabase("test")
                    coll     <- db.getCollection("watch4")
                    syncColl <- syncDb.getCollection("watch4")
                    csDocs   <- runChangeStream(syncColl).both(insert(coll)).map(_._1)
                yield expect(csDocs.size == 3).and(expect(csDocs.forall(d => Objects.isNull(d.getFullDocument))))
