package molly.core

import cats.effect.kernel.Async
import com.dimafeng.testcontainers.MongoDBContainer
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings

trait MollyTestSupport {

   def withClient[F[_]: Async, A](containers: MongoDBContainer)(run: MollyClient[F] => F[A]) = {
      val connectionString: String = containers.container.getConnectionString
      val settings: MongoClientSettings =
         MongoClientSettings.builder.applyConnectionString(new ConnectionString(connectionString)).build
      MollyClient.make[F](settings).use(run)
   }
}
