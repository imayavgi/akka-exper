import akka.actor.{Actor, ActorSystem, PoisonPill, Props}
import akka.routing.{ActorRefRoutee, Broadcast, RoundRobinRoutingLogic, Router}
import java.util.concurrent.CountDownLatch


// ====================
// ===== Messages =====
// ====================
sealed trait PiMessage

case object Calculate extends PiMessage

case class Work(start: Int, nrOfElements: Int) extends PiMessage

case class Result(value: Double) extends PiMessage

object PiCalculator extends App {

  val system = ActorSystem("piCalc")

  calculate(nrOfWorkers = 4, nrOfElements = 10000, nrOfMessages = 10000)

  // ==================
  // ===== Run it =====
  // ==================
  def calculate(nrOfWorkers: Int, nrOfElements: Int, nrOfMessages: Int) {

    // this latch is only plumbing to know when the calculation is completed
    val latch = new CountDownLatch(1)

    // create the master
    val master = system.actorOf(Props(classOf[Master], nrOfWorkers, nrOfMessages, nrOfElements, latch))

    // start the calculation
    master ! Calculate

    // wait for master to shut down
    latch.await()
  }
}

// ==================
// ===== Master =====
// ==================
class Master(
              nrOfWorkers: Int, nrOfMessages: Int, nrOfElements: Int, latch: CountDownLatch)
  extends Actor {

  var pi: Double = _
  var nrOfResults: Int = _
  var start: Long = _

  // create the workers
  var router = {
    val workers = Vector.fill(nrOfWorkers) {
      val r = context.actorOf(Props[Worker])
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), workers)
  }

  // message handler
  def receive = {
    case Calculate =>
      // schedule work
      //for (start <- 0 until nrOfMessages) router ! Work(start, nrOfElements)
      for (i <- 0 until nrOfMessages) router.route(Work(i * nrOfElements, nrOfElements), context.self)

    case Result(value) =>
      // handle result from the worker
      println(" Master got result from worker")
      pi += value
      nrOfResults += 1
      if (nrOfResults == nrOfMessages) context.stop(self)
  }

  override def preStart() {
    start = System.currentTimeMillis
  }

  override def postStop() {
    // tell the world that the calculation is complete
    println(
      "\n\tPi estimate: \t\t%s\n\tCalculation time: \t%s millis"
        .format(pi, (System.currentTimeMillis - start)))
    latch.countDown()
  }
}


// ==================
// ===== Worker =====
// ==================
class Worker extends Actor {

  // define the work
  def calculatePiFor(start: Int, nrOfElements: Int): Double = {
    var acc = 0.0
    for (i <- start until (start + nrOfElements))
      acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    acc
  }

  def receive = {
    case Work(start, nrOfElements) =>
      println(" Worker got work command " + sender)
      sender ! Result(calculatePiFor(start, nrOfElements)) // perform the work
  }
}


