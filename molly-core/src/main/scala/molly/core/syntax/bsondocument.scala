package molly.core.syntax

import molly.core.MollyCollection
import org.bson.BsonValue

import scala.reflect.ClassTag
import scala.reflect.classTag
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/** Syntactic sugar for [[org.bson.BsonDocument the Java driver's BsonDocument class]].
  */
trait bsondocument:

    type BsonDocumentCollection[F[_]] = MollyCollection[F, BsonDocument]

    type BsonDocument = org.bson.BsonDocument

    object BsonDocument:
        def apply(): BsonDocument = new BsonDocument()

        def apply(key: String, value: BsonValue) = new BsonDocument(key, value)

        def apply(elems: Iterable[(String, BsonValue)]): BsonDocument =
            val bsonDoc = new BsonDocument()
            elems.foreach(e => bsonDoc.put(e._1, e._2))
            bsonDoc

        def apply(elems: (String, BsonValue)*): BsonDocument =
            val bsonDoc = new BsonDocument()
            elems.foreach(e => bsonDoc.put(e._1, e._2))
            bsonDoc

    extension (bsonDoc: BsonDocument)

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

        /** If the value is not None, the key value pair will be inserted into the BsonDocument. If the value is None,
          * then the key field is going to be removed from the BsonDocument
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

        /** Read the value from the BsonDocument using the key. If the key does not exist or the type of the value is
          * not as expected, None will be returned
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

object bsondocument extends bsondocument
