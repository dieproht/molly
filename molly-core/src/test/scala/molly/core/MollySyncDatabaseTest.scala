package molly.core

import cats.effect.IO
import com.dimafeng.testcontainers.MongoDBContainer
import molly.core.syntax.bson.*
import molly.core.syntax.bsondocument.*
import org.testcontainers.utility.DockerImageName
import weaver.IOSuite

object MollySyncDatabaseTest extends IOSuite with TestContainerForAll[IO] with MollyTestSupport:
    override val containerDef: MongoDBContainer.Def = MongoDBContainer.Def(DockerImageName.parse(mongoVersion))

    test("listCollectionNames: return names of all collections in database"): containers =>
        withClient(containers): (client: MollyClient[IO]) =>
            withSyncClient(containers): (syncClient: MollySyncClient[IO]) =>
                val doc = new BsonDocument("_id", BsonInt32(1))
                for
                    db     <- client.getDatabase("test")
                    collA  <- db.getCollection("coll-A")
                    _      <- collA.insertOne(doc)
                    collB  <- db.getCollection("coll-B")
                    _      <- collB.insertOne(doc)
                    collC  <- db.getCollection("coll-C")
                    _      <- collC.insertOne(doc)
                    syncDb <- syncClient.getDatabase("test")
                    names  <- syncDb.listCollectionNames()
                yield expect(Set("coll-A", "coll-B", "coll-C") == names.toSet)
