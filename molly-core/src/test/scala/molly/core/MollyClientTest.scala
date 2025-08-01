package molly.core

import cats.effect.IO
import com.dimafeng.testcontainers.MongoDBContainer
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoTimeoutException
import com.mongodb.client.model.bulk.ClientNamespacedWriteModel
import molly.core.MollyCollectionTest.expect
import molly.core.MollyCollectionTest.test
import molly.core.MollyCollectionTest.withClient
import molly.core.syntax.bson.BsonInt32
import molly.core.syntax.bson.BsonString
import molly.core.syntax.bsondocument.*
import molly.core.syntax.mongo.MongoNamespace
import org.testcontainers.utility.DockerImageName
import weaver.IOSuite

import java.util.concurrent.TimeUnit

object MollyClientTest extends IOSuite, TestContainerForAll[IO], MollyTestSupport:
    override val containerDef: MongoDBContainer.Def = MongoDBContainer.Def(DockerImageName.parse(mongoVersion))

    test("raise error when database is unavailable"):
        val connectionString: ConnectionString = new ConnectionString("mongodb://localhost:20000")
        val settings: MongoClientSettings = MongoClientSettings.builder
            .applyToClusterSettings(_.serverSelectionTimeout(100, TimeUnit.MILLISECONDS).build)
            .applyConnectionString(connectionString)
            .build
        val program: IO[Unit] =
            MollyClient
                .make(settings)
                .use: client =>
                    for
                        db   <- client.getDatabase("test")
                        coll <- db.getCollection("test")
                        _    <- coll.insertOne(new BsonDocument("foo", new BsonString("bar")))
                    yield ()
        program.attempt.map(res => expect(res.left.map(_.getClass) == Left(classOf[MongoTimeoutException])))

    test("bulkWrite to collections"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            val doc1 = BsonDocument("_id" -> BsonString("abc-def-001"), "foo" -> BsonString("bar"))
            val expectedDoc1 = BsonDocument("_id" -> BsonString("abc-def-001"), "foo" -> BsonString("baz"))
            val doc2 = BsonDocument("_id" -> BsonString("abc-def-002"), "bar" -> BsonInt32(32))
            val doc3 = BsonDocument("_id" -> BsonString("abc-def-003"), "pika" -> BsonInt32(64))
            val writeModels = Seq(
              ClientNamespacedWriteModel.insertOne(
                MongoNamespace("test", "foo"),
                doc1
              ),
              ClientNamespacedWriteModel.insertOne(
                MongoNamespace("test", "bar"),
                doc2
              ),
              ClientNamespacedWriteModel.insertOne(
                MongoNamespace("test", "foo"),
                doc3
              ),
              ClientNamespacedWriteModel.updateOne(
                MongoNamespace("test", "foo"),
                BsonDocument("_id" -> BsonString("abc-def-001")),
                BsonDocument("$set" -> BsonDocument("foo" -> BsonString("baz")))
              ),
              ClientNamespacedWriteModel.deleteOne(
                MongoNamespace("test", "foo"),
                BsonDocument("_id" -> BsonString("abc-def-003"))
              )
            )

            for
                -       <- client.bulkWrite(writeModels)
                db      <- client.getDatabase("test")
                coll1   <- db.getCollection("foo")
                coll2   <- db.getCollection("bar")
                result1 <- coll1.find().list()
                result2 <- coll2.find().list()
            yield expect(result1.size == 1)
                .and(expect(result1.contains(expectedDoc1)))
                .and(expect(result2.size == 1))
                .and(expect(result2.contains(doc2)))
