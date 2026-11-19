package molly.core

import cats.effect.kernel.Resource
import com.dimafeng.testcontainers.MongoDBContainer
import weaver.FSuite

/** This integrates weaver-test with testcontainers-scala.
  */
trait TestContainerForAll[F[_]]:
    self: FSuite[F] =>

    val containerDef: MongoDBContainer.Def

    private def startContainer: MongoDBContainer =
        val container: MongoDBContainer = containerDef.createContainer()
        container.container.withReplicaSet().start()
        container

    final override type Res = containerDef.Container

    final override def sharedResource: Resource[F, Res] =
        Resource.make(effect.blocking(startContainer))(c => effect.blocking(c.stop()))
