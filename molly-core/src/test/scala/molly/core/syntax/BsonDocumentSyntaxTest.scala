package molly.core.syntax

import cats.syntax.option.*
import molly.core.bson.*
import molly.core.bsondocument.BsonDocument
import molly.core.syntax.bsondocument.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.Objects
import scala.jdk.CollectionConverters.*

class BsonDocumentSyntaxTest extends AnyFlatSpec with Matchers:
  private val embeddedDoc = new BsonDocument("key", new BsonInt32(1))

  private val embeddedArray = new BsonArray(
    Seq(new BsonString("1"), new BsonString("2"), new BsonString("3")).toList.asJava
  )

  private val testBsonDoc =
    new BsonDocument()
      .append("key1", embeddedDoc)
      .append("key2", embeddedArray)
      .append("key3", new BsonInt32(1))
      .append("key4", new BsonInt64(2))
      .append("key5", new BsonDecimal128(new Decimal128(3)))
      .append("key6", new BsonDouble(3.3))
      .append("key7", new BsonBoolean(true))
      .append("key8", new BsonString("molly"))
      .append("key9", new BsonDateTime(1024))
      .append("key10", new BsonTimestamp(2048))
      .append("key11", new BsonBinary("molly".getBytes()))
      .append("key12", new BsonNull)

  private val unknownKey: String = "undefined_key"

  "The 'append' extension" should "insert none empty value into document" in:
    val bsonDoc = BsonDocument().append("key1", None).append("key2", BsonString("value2").some)
    val expected = new BsonDocument("key2", new BsonString("value2"))
    bsonDoc shouldEqual expected

  it should "return the optional BsonDocument" in:
    testBsonDoc.getOption[BsonDocument]("key1") shouldEqual embeddedDoc.some
    testBsonDoc.getOption[BsonDocument]("key2") shouldEqual None
    testBsonDoc.getOption[BsonDocument](unknownKey) shouldEqual None

  it should "return the optional BsonArray" in:
    testBsonDoc.getOption[BsonArray]("key2") shouldEqual embeddedArray.some
    testBsonDoc.getOption[BsonArray]("key1") shouldEqual None
    testBsonDoc.getOption[BsonArray](unknownKey) shouldEqual None

  it should "return the optional BsonInt32" in:
    testBsonDoc.getOption[BsonInt32]("key3") shouldEqual BsonInt32(1).some
    testBsonDoc.getOption[BsonInt32]("key12") shouldEqual None
    testBsonDoc.getOption[BsonInt32](unknownKey) shouldEqual None

  it should "return the optional BsonInt64" in:
    testBsonDoc.getOption[BsonInt64]("key4") shouldEqual BsonInt64(2).some
    testBsonDoc.getOption[BsonInt64]("key12") shouldEqual None
    testBsonDoc.getOption[BsonInt64](unknownKey) shouldEqual None

  it should "return the optional BsonDecimal128" in:
    testBsonDoc.getOption[BsonDecimal128]("key5") shouldEqual BsonDecimal128(3).some
    testBsonDoc.getOption[BsonDecimal128]("key12") shouldEqual None
    testBsonDoc.getOption[BsonDecimal128](unknownKey) shouldEqual None

  it should "return the optional BsonDouble" in:
    testBsonDoc.getOption[BsonDouble]("key6") shouldEqual BsonDouble(3.3).some
    testBsonDoc.getOption[BsonDouble]("key12") shouldEqual None
    testBsonDoc.getOption[BsonDouble](unknownKey) shouldEqual None

  it should "return the optional BsonBoolean" in:
    testBsonDoc.getOption[BsonBoolean]("key7") shouldEqual BsonBoolean(true).some
    testBsonDoc.getOption[BsonBoolean]("key12") shouldEqual None
    testBsonDoc.getOption[BsonBoolean](unknownKey) shouldEqual None

  it should "return the optional BsonString" in:
    testBsonDoc.getOption[BsonString]("key8") shouldEqual BsonString("molly").some
    testBsonDoc.getOption[BsonString]("key12") shouldEqual None
    testBsonDoc.getOption[BsonString](unknownKey) shouldEqual None

  it should "return the optional BsonDateTime" in:
    testBsonDoc.getOption[BsonDateTime]("key9") shouldEqual BsonDateTime(1024).some
    testBsonDoc.getOption[BsonDateTime]("key12") shouldEqual None
    testBsonDoc.getOption[BsonDateTime](unknownKey) shouldEqual None

  it should "return the optional BsonTimestamp" in:
    testBsonDoc.getOption[BsonTimestamp]("key10") shouldEqual BsonTimestamp(2048).some
    testBsonDoc.getOption[BsonTimestamp]("key12") shouldEqual None
    testBsonDoc.getOption[BsonTimestamp](unknownKey) shouldEqual None

  it should "return the optional BsonBinary" in:
    testBsonDoc.getOption[BsonBinary]("key11") shouldEqual BsonBinary("molly".getBytes()).some
    testBsonDoc.getOption[BsonBinary]("key12") shouldEqual None
    testBsonDoc.getOption[BsonBinary](unknownKey) shouldEqual None

  "The 'settOption' extension" should "add a not None value to BsonDocument" in:
    val document = new BsonDocument
    val key = "key1"
    val value = new BsonString("abc-def-001")
    document.setOption(key, Some(value))
    document.get(key) shouldEqual value

  it should "not add a None value to BsonDocument" in:
    val document = new BsonDocument
    val key = "key1"
    document.setOption(key, None)
    Objects.nonNull(document.get(key)) shouldBe false

  it should "remove a key from BsonDocument, if called with value = None" in:
    val key = "key1"
    val document = new BsonDocument(key, new BsonString("333"))
    document.setOption(key, None)
    Objects.nonNull(document.get(key)) shouldBe false
