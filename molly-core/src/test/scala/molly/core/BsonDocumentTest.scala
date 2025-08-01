package molly.core

import molly.core.syntax.bson.BsonDateTime
import molly.core.syntax.bson.BsonDouble
import molly.core.syntax.bson.BsonInt32
import molly.core.syntax.bson.BsonString
import molly.core.syntax.bsondocument.BsonDocument
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BsonDocumentTest extends AnyFlatSpec, Matchers:
    "The BsonDocument companion" should "create a BsonDocument" in:
        BsonDocument() shouldEqual new BsonDocument()

        BsonDocument("key1" -> BsonString("1")) shouldEqual new BsonDocument("key1", new BsonString("1"))

        val expected = new BsonDocument("key1", new BsonInt32(1))
            .append("key2", new BsonString("2"))
            .append("key3", new BsonDateTime(1024))
            .append("key4", new BsonDouble(10.24))

        BsonDocument(
          "key1" -> BsonInt32(1),
          "key2" -> BsonString("2"),
          "key3" -> BsonDateTime(1024),
          "key4" -> BsonDouble(10.24)
        ) shouldEqual expected

        BsonDocument(
          Seq(
            "key1" -> BsonInt32(1),
            "key2" -> BsonString("2"),
            "key3" -> BsonDateTime(1024),
            "key4" -> BsonDouble(10.24)
          )
        ) shouldEqual expected
