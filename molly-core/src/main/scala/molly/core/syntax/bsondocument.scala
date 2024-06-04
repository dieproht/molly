package molly.core.syntax

import org.bson.*

import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.reflect.classTag
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/** Syntax extensions for [[org.bson.BsonDocument the Java driver's BsonDocument class]].
  */
trait bsondocument:
  implicit def mollyBsonDocumentSyntax(bsonDoc: BsonDocument): BsonDocumentExtensions =
    new BsonDocumentExtensions(bsonDoc)

object bsondocument extends bsondocument

final class BsonDocumentExtensions(bsonDoc: BsonDocument):

  /** Append an optional value with the given key to the document.
    *
    * @param key
    *   The key field
    * @param valueOpt
    *   An Option BsonValue(@see org.bson.BsonValue), only if the value is defined then the key value pair will be
    *   inserted into the BsonDocument
    *
    * @return
    *   this BsonDocument
    */
  def append(key: String, valueOpt: Option[BsonValue]): BsonDocument =
    valueOpt.fold(bsonDoc)(v => bsonDoc.append(key, v))

  /** If the value is not None, the key value pair will be inserted into the BsonDocument. If the value is None, then
    * the key field is going to be removed from the BsonDocument
    *
    * @param key
    *   The key field
    * @param value
    *   An Option BsonValue(@see org.bson.BsonValue)
    * @return
    *   this BsonDocument
    */
  def setOption(key: String, value: Option[BsonValue]): BsonDocument =
    value.fold {
      bsonDoc.remove(key)
      bsonDoc
    }(v => bsonDoc.append(key, v))

  /** Read the value from the BsonDocument using the key. If the key does not exist or the type of the value is not as
    * expected, None will be returned
    *
    * @tparam T
    *   the type parameter for the expected value
    * @param key
    *   The key field
    * @return
    *   optional value of type T
    */
  def getOption[T <: BsonValue: ClassTag](key: String): Option[T] =
    if bsonDoc.containsKey(key) then
      Try(classTag[T].runtimeClass.cast(bsonDoc.get(key))) match
        case Success(v) =>
          Some(v.asInstanceOf[T])
        case Failure(_) => None
    else None
