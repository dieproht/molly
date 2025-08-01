package molly.core.syntax

trait mongo:
    type MongoNamespace = com.mongodb.MongoNamespace

    object MongoNamespace:
        def apply(dbName: String, collName: String): MongoNamespace = new MongoNamespace(dbName, collName)

object mongo extends mongo
