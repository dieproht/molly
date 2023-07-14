package molly.core

import cats.effect.kernel.Resource
import cats.effect.kernel.Sync
import com.dimafeng.testcontainers.ContainerDef
import weaver.MutableFSuite

/** This integrates weaver-test with testcontainers-scala.
  */
trait TestContainerForAll[F[_]] { self: MutableFSuite[F] =>

   val containerDef: ContainerDef

   final override type Res = containerDef.Container

   final override def sharedResource: Resource[F, Res] =
      Resource.make(Sync[F].blocking(containerDef.start()))(c => Sync[F].blocking(c.stop()))
}
