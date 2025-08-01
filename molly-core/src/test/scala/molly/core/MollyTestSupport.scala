package molly.core

import cats.effect.kernel.Async
import com.dimafeng.testcontainers.MongoDBContainer
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings

trait MollyTestSupport:

    def mongoVersion: String = "mongo:8.0"

    def withClient[F[_], A](containers: MongoDBContainer)(run: MollyClient[F] => F[A])(using Async[F]): F[A] =
        val connectionString: String = containers.container.getConnectionString
        val settings: MongoClientSettings =
            MongoClientSettings.builder.applyConnectionString(new ConnectionString(connectionString)).build
        MollyClient.make[F](settings).use(run)

    def withSyncClient[F[_], A](containers: MongoDBContainer)(run: MollySyncClient[F] => F[A])(using Async[F]): F[A] =
        val connectionString: String = containers.container.getConnectionString
        val settings: MongoClientSettings =
            MongoClientSettings.builder.applyConnectionString(new ConnectionString(connectionString)).build
        MollySyncClient.make[F](settings).use(run)
