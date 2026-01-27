package molly.core

import cats.effect.kernel.Resource
import cats.effect.kernel.Sync
import com.dimafeng.testcontainers.MongoDBContainer
import weaver.MutableFSuite

/** This integrates weaver-test with testcontainers-scala.
  */
trait TestContainerForAll[F[_]](using f: Sync[F]):
    self: MutableFSuite[F] =>

    val containerDef: MongoDBContainer.Def

    private def startContainer: MongoDBContainer =
        val container: MongoDBContainer = containerDef.createContainer()
        container.container.withReplicaSet().start()
        container

    final override type Res = containerDef.Container

    final override def sharedResource: Resource[F, Res] =
        Resource.make(f.blocking(startContainer))(c => f.blocking(c.stop()))
