import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.ExecutionContext.Implicits.global

object Step1 {

  def main(args: Array[String]) {

    // Bootstrapping boilerplate
    implicit val system = ActorSystem("flow")
    implicit val materializer = ActorMaterializer()

    // Construct the parts
    val source = Source(1 to 10)
    val doubler = Flow[Int].map(_ * 2)
    val filter = Flow[Int].filter(_ > 10)
    val sink = Sink.foreach(println)

    // Hook them up and run
    source
      .via(doubler)
      .via(filter)
      .runWith(sink)
      .andThen { case _ => system.shutdown() }
  }

}
