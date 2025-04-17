package molly.core

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.functor.*
import com.mongodb.ReadConcern
import com.mongodb.ReadPreference
import com.mongodb.WriteConcern
import com.mongodb.client.MongoDatabase
import fs2.Stream
import org.bson.BsonDocument

import scala.jdk.CollectionConverters.*

/** Molly's counterpart to
  * [[https://mongodb.github.io/mongo-java-driver/5.4/apidocs/mongodb-driver-sync/com/mongodb/client/MongoDatabase.html MongoDatabase]]
  */
final case class MollySyncDatabase[F[_]] private[core] (private[core] val delegate: MongoDatabase)(using f: Async[F]):

    /** [[https://mongodb.github.io/mongo-java-driver/5.4/apidocs/mongodb-driver-sync/com/mongodb/client/MongoDatabase.html#getCollection(java.lang.String)]]
      */
    def getCollection(collectionName: String): F[MollySyncCollection[F, BsonDocument]] =
        f.delay(delegate.getCollection(collectionName, classOf[BsonDocument])).map(MollySyncCollection(_))

    /** Like [[this.getCollection]], but returns a
      * [[https://typelevel.org/cats-effect/api/3.x/cats/effect/kernel/Resource.html Resource]]
      */
    def getCollectionAsResource(collectionName: String): Resource[F, MollySyncCollection[F, BsonDocument]] =
        Resource.eval(getCollection(collectionName))

    /** Like [[this.getCollection]], but maps documents to type a using the given codec.
      */
    def getTypedCollection[A](collectionName: String)(using MollyCodec[F, A]): F[MollySyncCollection[F, A]] =
        f.delay(delegate.getCollection(collectionName, classOf[BsonDocument])).map(MollySyncCollection[F, A](_))

    /** Like [[this.getTypedCollection]], but returns a
      * [[https://typelevel.org/cats-effect/api/3.x/cats/effect/kernel/Resource.html Resource]]
      */
    def getTypedCollectionAsResource[A](collectionName: String)(using
        MollyCodec[F, A]
    ): Resource[F, MollySyncCollection[F, A]] =
        Resource.eval(getTypedCollection(collectionName))

    /** [[https://mongodb.github.io/mongo-java-driver/5.4/apidocs/mongodb-driver-sync/com/mongodb/client/MongoDatabase.html#listCollectionNames()]]
      */
    def listCollectionNames(): F[List[String]] =
        Stream
            .bracket(f.delay(delegate.listCollectionNames().cursor()))(cursor => f.delay(cursor.close()))
            .flatMap(cursor => Stream.fromIterator(cursor.asScala, 1))
            .compile
            .toList

    /** [[https://mongodb.github.io/mongo-java-driver/5.4/apidocs/mongodb-driver-sync/com/mongodb/client/MongoDatabase.html#getReadConcern()]]
      */
    def getReadConcern(): ReadConcern = delegate.getReadConcern()

    /** [[https://mongodb.github.io/mongo-java-driver/5.4/apidocs/mongodb-driver-sync/com/mongodb/client/MongoDatabase.html#getReadPreference()]]
      */
    def getReadPreference(): ReadPreference = delegate.getReadPreference()

    /** [[https://mongodb.github.io/mongo-java-driver/5.4/apidocs/mongodb-driver-sync/com/mongodb/client/MongoDatabase.html#getWriteConcern()]]
      */
    def getWriteConcern(): WriteConcern = delegate.getWriteConcern()

    /** [[https://mongodb.github.io/mongo-java-driver/5.4/apidocs/mongodb-driver-sync/com/mongodb/client/MongoDatabase.html#withReadConcern(com.mongodb.ReadConcern)]]
      */
    def withReadConcern(readConcern: ReadConcern): MollySyncDatabase[F] =
        MollySyncDatabase(delegate.withReadConcern(readConcern))

    /** [[https://mongodb.github.io/mongo-java-driver/5.4/apidocs/mongodb-driver-sync/com/mongodb/client/MongoDatabase.html#withReadPreference(com.mongodb.ReadPreference)]]
      */
    def withReadPreference(readPreference: ReadPreference): MollySyncDatabase[F] =
        MollySyncDatabase(delegate.withReadPreference(readPreference))

    /** [[https://mongodb.github.io/mongo-java-driver/5.4/apidocs/mongodb-driver-sync/com/mongodb/client/MongoDatabase.html#withWriteConcern(com.mongodb.WriteConcern)]]
      */
    def withWriteConcern(writeConcern: WriteConcern): MollySyncDatabase[F] =
        MollySyncDatabase(delegate.withWriteConcern(writeConcern))
