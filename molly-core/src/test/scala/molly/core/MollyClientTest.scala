package molly.core

import cats.effect.IO
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoTimeoutException
import org.bson.BsonDocument
import org.bson.BsonString
import weaver.SimpleIOSuite

import java.util.concurrent.TimeUnit

object MollyClientTest extends SimpleIOSuite:

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
