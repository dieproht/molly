package molly.core.syntax

import scala.jdk.CollectionConverters.*

/** Syntactic sugar for
  * [[https://mongodb.github.io/mongo-java-driver/5.2/apidocs/bson/org/bson/package-summary.html the Java driver's bson classes]].
  */
trait bson:
  type BsonArray = org.bson.BsonArray

  type BsonValue = org.bson.BsonValue

  object BsonArray:

    def apply(): BsonArray = new BsonArray()

    def apply(elems: Iterable[BsonValue]): BsonArray = new BsonArray(elems.toList.asJava)

  type BsonBinary = org.bson.BsonBinary

  object BsonBinary:

    def apply(value: Array[Byte]): BsonBinary = new BsonBinary(value)

  type BsonBoolean = org.bson.BsonBoolean

  object BsonBoolean:

    def apply(value: Boolean): BsonBoolean = new BsonBoolean(value)

  type BsonDateTime = org.bson.BsonDateTime

  object BsonDateTime:

    def apply(value: Long): BsonDateTime = new BsonDateTime(value)

  type BsonDecimal128 = org.bson.BsonDecimal128

  type Decimal128 = org.bson.types.Decimal128

  object BsonDecimal128:

    def apply(value: Decimal128): BsonDecimal128 = new BsonDecimal128(value)

    def apply(value: Long): BsonDecimal128 = apply(new Decimal128(value))

  type BsonDouble = org.bson.BsonDouble

  object BsonDouble:

    def apply(value: Double): BsonDouble = new BsonDouble(value)

  type BsonInt32 = org.bson.BsonInt32

  object BsonInt32:

    def apply(value: Int): BsonInt32 = new BsonInt32(value)

  type BsonInt64 = org.bson.BsonInt64

  object BsonInt64:

    def apply(value: Long): BsonInt64 = new BsonInt64(value)

  type BsonNull = org.bson.BsonNull

  object BsonNull:

    def apply(): BsonNull = new BsonNull()

  type BsonObjectId = org.bson.BsonObjectId

  type ObjectId = org.bson.types.ObjectId

  object BsonObjectId:

    def apply(): BsonObjectId = new BsonObjectId()

    def apply(value: ObjectId): BsonObjectId = new BsonObjectId(value)

  type BsonRegularExpression = org.bson.BsonRegularExpression

  object BsonRegularExpression:

    def apply(value: String): BsonRegularExpression = new BsonRegularExpression(value)

    def apply(value: String, options: String): BsonRegularExpression = new BsonRegularExpression(value, options)

  type BsonString = org.bson.BsonString

  object BsonString:

    def apply(value: String): BsonString = new BsonString(value)

  type BsonTimestamp = org.bson.BsonTimestamp

  object BsonTimestamp:

    def apply(): BsonTimestamp = new BsonTimestamp()

    def apply(value: Long): BsonTimestamp = new BsonTimestamp(value)

    def apply(seconds: Int, increment: Int): BsonTimestamp = new BsonTimestamp(seconds, increment)

  type BsonUndefined = org.bson.BsonUndefined

  object BsonUndefined:

    def apply(): BsonUndefined = new BsonUndefined()

object bson extends bson
