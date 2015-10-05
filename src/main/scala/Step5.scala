import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.stream.scaladsl._
import akka.util.Timeout
import com.lambdaworks.crypto.SCryptUtil
import net.fehmicansaglam.bson.BsonDsl._
import net.fehmicansaglam.bson.{BsonDocument, Bulk}
import net.fehmicansaglam.tepkin.MongoClient

import scala.concurrent.duration._


object Step5 {

  def main(args: Array[String]) {

    // Bootstrapping boilerplate
    implicit val system = ActorSystem("flow")
    implicit val materializer = ActorMaterializer()
    implicit val timeout = Timeout(1.minute)

    // Connect to DB and obtain collection
    val client = MongoClient("mongodb://localhost", context = system)
    val db = client.db("tepkin-demo")
    val usersCollection = db.collection("users")
    val hashedCollection = db.collection("hashed")


    // Query
    val source = usersCollection.find(BsonDocument.empty)

    // Flatten
    val queryResults = source.mapConcat(identity)

    // Hash
    val hashFlow = Flow[BsonDocument]
      .map(hash)
      .grouped(25)
      .map(s => Bulk(s.toList))

    val sink = hashedCollection.sink()

    // Construct graph
    val graph = FlowGraph.closed(sink) {
      implicit builder => sink =>
        val balancer = builder.add(Balance[BsonDocument](8))
        val merge = builder.add(Merge[Bulk](8))

        queryResults ~> balancer
        merge ~> sink

        for (i <- 0 until 8)
          balancer.out(i) ~> hashFlow ~> merge.in(i)
    }


    // Run the graph
    val worker = graph.run()

    client.shutdown(worker)
  }

  def hash(doc: BsonDocument) = {
    val Some(pw) = doc.getAs[String]("password")
    val hash = SCryptUtil.scrypt(pw, 16384, 8, 1)
    doc ~ ("password" := hash)
  }

}
