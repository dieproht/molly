package molly.core

import molly.core.bson.*
import org.bson.types.Decimal128
import org.bson.types.ObjectId
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters.*
class BsonValueTest extends AnyFlatSpec with Matchers {

   "The BsonArray companion object" should "create a BsonArray" in {
      BsonArray() shouldEqual new BsonArray()

      val input = Seq(new BsonString("a"), new BsonString("b"), new BsonString("c"))
      val bsonArray = BsonArray(input)
      val expectedArray = new BsonArray(input.toList.asJava)

      bsonArray shouldEqual expectedArray
   }

   "The BsonBinary companion object" should "create a BsonBinary" in {
      val bytesArray = "molly".getBytes
      BsonBinary(bytesArray) shouldEqual new BsonBinary(bytesArray)
   }

   "The BsonBoolean companion object" should "create a BsonBoolean" in {
      BsonBoolean(true) shouldEqual new BsonBoolean(true)
      BsonBoolean(false) shouldEqual new BsonBoolean(false)
   }

   "The BsonDateTime companion object" should "create a BsonDateTime" in {
      BsonDateTime(1024L) shouldEqual new BsonDateTime(1024L)
   }

   "The BsonDecimal128 companion object" should "create a BsonDecimal128" in {
      val value = new Decimal128(2048)
      BsonDecimal128(value) shouldEqual new BsonDecimal128(value)
      BsonDecimal128(2048L) shouldEqual new BsonDecimal128(value)
   }

   "The BsonDouble companion object" should "create a BsonDouble" in {
      BsonDouble(10.24) shouldEqual new BsonDouble(10.24)
   }

   "The BsonInt32 companion object" should "create a BsonInt32" in {
      BsonInt32(512) shouldEqual new BsonInt32(512)
   }

   "The BsonInt64 companion object" should "create a BsonInt64" in {
      BsonInt64(1024) shouldEqual new BsonInt64(1024)
   }

   "The BsonNull companion object" should "create a BsonNull" in {
      BsonNull() shouldEqual new BsonNull()
   }

   "The BsonObjectId companion object" should "create a BsonObjectId" in {
      val value = new ObjectId()
      BsonObjectId(value) shouldEqual new BsonObjectId(value)
   }

   "The BsonRegularExpression companion object" should "create a BsonRegularExpression" in {
      BsonRegularExpression("/(.*)/") shouldEqual new BsonRegularExpression("/(.*)/")
      BsonRegularExpression("/(.*)/", "?i") shouldEqual new BsonRegularExpression("/(.*)/", "?i")
   }

   "The BsonString companion object" should "create a BsonString" in {
      val value = "molly ist geil"
      BsonString(value) shouldEqual new BsonString(value)
   }

   "The BsonTimestamp companion object" should "create a BsonTimestamp" in {
      BsonTimestamp() shouldEqual new BsonTimestamp()
      BsonTimestamp(1024L) shouldEqual new BsonTimestamp(1024L)
      BsonTimestamp(10, 24) shouldEqual new BsonTimestamp(10, 24)
   }

   "The BsonUndefined companion object" should "create a BsonUndefined" in {
      BsonUndefined() shouldEqual new BsonUndefined()
   }
}
