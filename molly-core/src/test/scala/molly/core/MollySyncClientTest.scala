package molly.core

import cats.effect.IO
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoTimeoutException
import org.bson.BsonDocument
import weaver.SimpleIOSuite

import java.util.concurrent.TimeUnit

object MollySyncClientTest extends SimpleIOSuite:

  test("raise error when database is unavailable"):
    val connectionString: ConnectionString = new ConnectionString("mongodb://localhost:20000")
    val settings: MongoClientSettings = MongoClientSettings.builder
      .applyToClusterSettings(_.serverSelectionTimeout(100, TimeUnit.MILLISECONDS).build)
      .applyConnectionString(connectionString)
      .build
    val program: IO[Unit] =
      MollySyncClient
        .make(settings)
        .use: client =>
          for
            db   <- client.getDatabase("test")
            coll <- db.getCollection("test")
            _    <- coll.watch().list(1)
          yield ()
    program.attempt.map(res => expect(res.left.map(_.getClass) == Left(classOf[MongoTimeoutException])))
