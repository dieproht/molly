package molly.medeia

import cats.effect.IO
import com.dimafeng.testcontainers.MongoDBContainer
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.client.model.changestream.FullDocument
import medeia.codec.*
import molly.core.MollyClient
import molly.core.MollyCollection
import molly.core.MollyTestSupport
import molly.core.TestContainerForAll
import molly.core.model.FindOneAndReplaceOptions
import molly.core.model.ReplaceOptions
import org.testcontainers.utility.DockerImageName
import weaver.IOSuite
import scala.concurrent.duration.*

object TypedMollyCollectionTest extends IOSuite with TestContainerForAll[IO] with MollyTestSupport {

   case class City(name: String, state: String, area: Double, postalCodes: List[String])

   implicit val cityCodec: BsonDocumentCodec[City] = BsonDocumentCodec.derived

   import molly.medeia.codec.*

   val trier = City(
      name = "Trier",
      state = "Rhineland-Palatinate",
      area = 117.06,
      postalCodes = List("54290", "54292", "54293", "54294", "54295", "54296")
   )

   val ludwigslust =
      City(name = "Ludwigslust", state = "Mecklenburg-Vorpommern", area = 78.3, postalCodes = List("19288"))

   val flensburg = City(
      name = "Flensburg",
      state = "Schleswig-Holstein",
      area = 56.73,
      postalCodes = List("24937", "24938", "24939", "24940", "24941", "24942", "24943", "24944")
   )

   override def maxParallelism: Int = 1

   override val containerDef: MongoDBContainer.Def = MongoDBContainer.Def(DockerImageName.parse("mongo:7.0"))

   test("deleteMany: delete given documents from collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("deleteMany")
            _       <- coll.insertMany(Seq(trier, ludwigslust, flensburg))
            _       <- coll.deleteMany(Filters.in("name", "Trier", "Flensburg"))
            results <- coll.find().list()
         } yield expect(results.size == 1)
            .and(expect(results.contains(ludwigslust)))
      }
   }

   test("deleteOne: delete one document from collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("deleteOne")
            _       <- coll.insertMany(Seq(trier, ludwigslust))
            _       <- coll.deleteOne(Filters.eq("name", "Trier"))
            results <- coll.find().list()
         } yield expect(results.size == 1)
            .and(expect(results.contains(ludwigslust)))
      }
   }

   test("find: return all documents from collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("find2")
            _       <- coll.insertMany(Seq(trier, ludwigslust, flensburg))
            results <- coll.find().list()
         } yield expect(results.size == 3)
            .and(expect(results.contains(trier)))
            .and(expect(results.contains(ludwigslust)))
            .and(expect(results.contains(flensburg)))
      }
   }

   test("find first: return first found document from collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db     <- client.getDatabase("test")
            coll   <- db.getTypedCollection[City]("find3")
            _      <- coll.insertMany(Seq(trier, ludwigslust, flensburg))
            result <- coll.find().first
         } yield expect(result.isDefined)
            .and(
               expect(result.contains(trier))
                  .or(expect(result.contains(ludwigslust)))
                  .or(expect(result.contains(flensburg)))
            )
      }
   }

   test("find first: return no document when there is no match") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db     <- client.getDatabase("test")
            coll   <- db.getTypedCollection[City]("find4")
            _      <- coll.insertMany(Seq(trier, ludwigslust, flensburg))
            result <- coll.find(Filters.eq("name", "Bielefeld")).first
         } yield expect(result.isEmpty)
      }
   }

   test("find: return all documents matching the given filter") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("find5")
            _       <- coll.insertMany(Seq(trier, ludwigslust, flensburg))
            results <- coll.find(Filters.gt("area", 70)).list()
         } yield expect(results.size == 2)
            .and(expect(results.contains(trier)))
            .and(expect(results.contains(ludwigslust)))
      }
   }

   test("find: return all documents matching the given chained filter") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("find6")
            _       <- coll.insertMany(Seq(trier, ludwigslust, flensburg))
            results <- coll.find().filter(Filters.gt("area", 70)).list()
         } yield expect(results.size == 2)
            .and(expect(results.contains(trier)))
            .and(expect(results.contains(ludwigslust)))
      }
   }

   test("findOneAndDelete: return one document and delete it from collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("findOneAndDelete1")
            _       <- coll.insertMany(Seq(trier, ludwigslust))
            resDoc  <- coll.findOneAndDelete(Filters.eq("name", "Trier"))
            resColl <- coll.find().list()
         } yield expect(resDoc == Some(trier))
            .and(expect(resColl.size == 1))
            .and(expect(resColl.contains(ludwigslust)))
      }
   }

   test("findOneAndDelete: return and delete nothing if nothing matches the given filter") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("findOneAndDelete2")
            _       <- coll.insertMany(Seq(trier, ludwigslust))
            resDoc  <- coll.findOneAndDelete(Filters.eq("name", "Bielefeld"))
            resColl <- coll.find().list()
         } yield expect(resDoc == None)
            .and(expect(resColl.size == 2))
            .and(expect(resColl.contains(trier)))
            .and(expect(resColl.contains(ludwigslust)))
      }
   }

   test("findOneAndReplace: return one document and replace it in collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val largerLudwigslust = ludwigslust.copy(area = 100)
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("findOneAndReplace")
            _       <- coll.insertMany(Seq(trier, ludwigslust))
            resDoc  <- coll.findOneAndReplace(Filters.eq("name", "Ludwigslust"), largerLudwigslust)
            resColl <- coll.find().list()
         } yield expect(resDoc == Some(ludwigslust))
            .and(expect(resColl.size == 2))
            .and(expect(resColl.contains(trier)))
            .and(expect(resColl.contains(largerLudwigslust)))
      }
   }

   test("findOneAndReplace: return and replace nothing if nothing matches the given filter") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("findOneAndReplace1")
            _       <- coll.insertMany(Seq(trier, ludwigslust))
            resDoc  <- coll.findOneAndReplace(Filters.eq("name", "Flensburg"), flensburg)
            resColl <- coll.find().list()
         } yield expect(resDoc == None)
            .and(expect(resColl.size == 2))
            .and(expect(resColl.contains(trier)))
            .and(expect(resColl.contains(ludwigslust)))
            .and(expect(!resColl.contains(flensburg)))
      }
   }

   test("findOneAndReplace: return one document and replace it in collection - insert if it doesn't exist") {
      containers =>
         withClient(containers) { (client: MollyClient[IO]) =>
            for {
               db   <- client.getDatabase("test")
               coll <- db.getTypedCollection[City]("findOneAndReplace2")
               _    <- coll.insertOne(trier)
               resDoc <- coll.findOneAndReplace(
                  Filters.eq("name", "Ludwigslust"),
                  ludwigslust,
                  FindOneAndReplaceOptions().upsert(true)
               )
               resColl <- coll.find().list()
            } yield expect(resDoc == None)
               .and(expect(resColl.size == 2))
               .and(expect(resColl.contains(trier)))
               .and(expect(resColl.contains(ludwigslust)))
         }
   }

   test("findOneAndReplace: return one document and replace it in collection - do not insert if it doesn't exist") {
      containers =>
         withClient(containers) { (client: MollyClient[IO]) =>
            for {
               db   <- client.getDatabase("test")
               coll <- db.getTypedCollection[City]("findOneAndReplace3")
               _    <- coll.insertOne(trier)
               resDoc <- coll.findOneAndReplace(
                  Filters.eq("name", "Ludwigslust"),
                  ludwigslust,
                  FindOneAndReplaceOptions().upsert(false)
               )
               resColl <- coll.find().list()
            } yield expect(resDoc == None)
               .and(expect(resColl.size == 1))
               .and(expect(resColl.contains(trier)))
               .and(expect(!resColl.contains(ludwigslust)))
         }
   }

   test("findOneAndUpdate: return one document and update it in collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val largerFlensburg = flensburg.copy(area = 105.5)
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("findOneAndUpdate1")
            _       <- coll.insertMany(Seq(trier, flensburg))
            resDoc  <- coll.findOneAndUpdate(Filters.eq("name", "Flensburg"), Updates.set("area", 105.5))
            resColl <- coll.find().list()
         } yield expect(resDoc == Some(flensburg))
            .and(expect(resColl.size == 2))
            .and(expect(resColl.contains(trier)))
            .and(expect(resColl.contains(largerFlensburg)))
      }
   }

   test("findOneAndUpdate: return and update nothing if nothing matches the given filter") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("findOneAndUpdate2")
            _       <- coll.insertMany(Seq(trier, ludwigslust))
            resDoc  <- coll.findOneAndUpdate(Filters.eq("name", "Flensburg"), Updates.set("area", 100))
            resColl <- coll.find().list()
         } yield expect(resDoc == None)
            .and(expect(resColl.size == 2))
            .and(expect(resColl.contains(trier)))
            .and(expect(resColl.contains(ludwigslust)))
            .and(expect(!resColl.contains(flensburg)))
      }
   }

   test("insertMany: write given documents to collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("insertMany")
            _       <- coll.insertMany(Seq(trier, ludwigslust, flensburg))
            results <- coll.find().list()
         } yield expect(results.size == 3)
            .and(expect(results.contains(trier)))
            .and(expect(results.contains(ludwigslust)))
            .and(expect(results.contains(flensburg)))
      }
   }

   test("insertOne: write one document to collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("insertOne")
            _       <- coll.insertOne(trier)
            results <- coll.find().list()
         } yield expect(results.size == 1)
            .and(expect(results.contains(trier)))
      }
   }

   test("replaceOne: replace one document in collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val largerLudwigslust = ludwigslust.copy(area = 100)
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("replaceOne1")
            _       <- coll.insertMany(Seq(trier, ludwigslust))
            res     <- coll.replaceOne(Filters.eq("name", "Ludwigslust"), largerLudwigslust)
            results <- coll.find().list()
         } yield expect(results.size == 2)
            .and(expect(results.contains(trier)))
            .and(expect(!results.contains(ludwigslust)))
            .and(expect(results.contains(largerLudwigslust)))
      }
   }

   test("replaceOne: replace one document in collection - insert if it doesn't exist") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("replaceOne2")
            _       <- coll.insertMany(Seq(trier))
            res     <- coll.replaceOne(Filters.eq("name", "Ludwigslust"), ludwigslust, ReplaceOptions().upsert(true))
            results <- coll.find().list()
         } yield expect(results.size == 2)
            .and(expect(results.contains(trier)))
            .and(expect(results.contains(ludwigslust)))
      }
   }

   test("replaceOne: replace one document in collection - do not insert if it doesn't exist") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         for {
            db   <- client.getDatabase("test")
            coll <- db.getTypedCollection[City]("replaceOne3")
            _    <- coll.insertMany(Seq(trier))
            res  <- coll.replaceOne(Filters.eq("name", ludwigslust.name), ludwigslust, ReplaceOptions().upsert(false))
            results <- coll.find().list()
         } yield expect(results.size == 1)
            .and(expect(results.contains(trier)))
            .and(expect(!results.contains(ludwigslust)))
      }
   }

   test("updateMany: update multiple documents in collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val largerLudwigslust = ludwigslust.copy(area = 80.5)
         val largerFlensburg = flensburg.copy(area = 80.5)
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("updateMany")
            _       <- coll.insertMany(Seq(trier, ludwigslust, flensburg))
            res     <- coll.updateMany(Filters.in("name", "Ludwigslust", "Flensburg"), Updates.set("area", 80.5))
            results <- coll.find().list()
         } yield expect(results.size == 3)
            .and(expect(results.contains(trier)))
            .and(expect(!results.contains(ludwigslust)))
            .and(expect(results.contains(largerLudwigslust)))
            .and(expect(!results.contains(flensburg)))
            .and(expect(results.contains(largerFlensburg)))
      }
   }

   test("updateOne: update one document in collection") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val largerLudwigslust = ludwigslust.copy(area = 80.5)
         for {
            db      <- client.getDatabase("test")
            coll    <- db.getTypedCollection[City]("updateOne")
            _       <- coll.insertMany(Seq(trier, ludwigslust, flensburg))
            res     <- coll.updateOne(Filters.eq("name", "Ludwigslust"), Updates.set("area", 80.5))
            results <- coll.find().list()
         } yield expect(results.size == 3)
            .and(expect(results.contains(trier)))
            .and(expect(!results.contains(ludwigslust)))
            .and(expect(results.contains(largerLudwigslust)))
            .and(expect(results.contains(flensburg)))
      }
   }

   private val eta = 200.millis

   test("watch: return one change per inserted document") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>

         def runChangeStream(coll: MollyCollection[IO, City]) =
            coll.watch().stream(bufferSize = 1).take(3).compile.toList

         def insert(coll: MollyCollection[IO, City]) =
            IO.sleep(eta) >> coll.insertMany(Seq(trier, ludwigslust, flensburg))

         for {
            db     <- client.getDatabase("test")
            coll   <- db.getTypedCollection[City]("watch1")
            csDocs <- runChangeStream(coll).both(insert(coll)).map(_._1)
         } yield expect(csDocs.size == 3)
            .and(expect(csDocs.exists(_.getFullDocument == trier)))
            .and(expect(csDocs.exists(_.getFullDocument == ludwigslust)))
            .and(expect(csDocs.exists(_.getFullDocument == flensburg)))
            .and(expect(csDocs.forall(_.getOperationTypeString() == "insert")))
      }
   }

   test("watch: return different changes") { containers =>
      withClient(containers) { (client: MollyClient[IO]) =>
         val largerLudwigslust = ludwigslust.copy(area = 100.3)

         def runChangeStream(coll: MollyCollection[IO, City]) =
            coll
               .watch()
               .fullDocument(FullDocument.UPDATE_LOOKUP)
               .stream(bufferSize = 1)
               .take(4)
               .compile
               .toList

         def insertAndUpdate(coll: MollyCollection[IO, City]) =
            IO.sleep(eta) >> coll.insertMany(Seq(trier, ludwigslust, flensburg)) >> coll.updateOne(
               Filters.eq("name", "Ludwigslust"),
               Updates.set("area", 100.3)
            )

         for {
            db     <- client.getDatabase("test")
            coll   <- db.getTypedCollection[City]("watch2")
            csDocs <- runChangeStream(coll).both(insertAndUpdate(coll)).map(_._1)
         } yield expect(csDocs.size == 4)
            .and(expect(csDocs.exists(_.getFullDocument == trier)))
            .and(expect(csDocs.exists(_.getFullDocument == ludwigslust)))
            .and(expect(csDocs.exists(_.getFullDocument == flensburg)))
            .and(expect(csDocs.exists(_.getFullDocument == largerLudwigslust)))
            .and(expect(csDocs.take(3).forall(_.getOperationTypeString() == "insert")))
            .and(expect(csDocs.last.getOperationTypeString() == "update"))
      }
   }
}
