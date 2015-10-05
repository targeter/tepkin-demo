import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import net.fehmicansaglam.bson.BsonDsl._
import net.fehmicansaglam.bson.Bulk
import net.fehmicansaglam.tepkin.MongoClient

import scala.util.Random


object Step2 {

  def main(args: Array[String]) {

    // Bootstrapping boilerplate
    implicit val system = ActorSystem("flow")
    implicit val materializer = ActorMaterializer()

    // Connect to DB and obtain collection
    val client = MongoClient("mongodb://localhost", context = system)
    val db = client.db("tepkin-demo")
    val collection = db.collection("users")


    // Document generator
    val usernameIterator = Iterator.from(1).map("username" + _)
    val passwordIterator = Iterator.continually(randomString(10))
    val userDocumentGenerator = usernameIterator.zip(passwordIterator).map {
      case (username, password) =>
        ("username" := username) ~ ("password" := password)
    }


    // Create bulks of 1000
    val grouped = userDocumentGenerator.grouped(1000).map(_.toList)
    val source = Source(() => grouped).map(Bulk)


    // insert 50 * 1000 = 50 000 users
    val within = source.take(1000)

    val worker = within.toMat(collection.sink(parallelism = 3))(Keep.right).run()

    client.shutdown(worker)
  }


  def randomString(size: Int): String = {
    String.valueOf(Random.alphanumeric.take(size).toArray)
  }
}
