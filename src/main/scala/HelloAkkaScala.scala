import akka.actor.{ ActorRef, ActorSystem, Props, Actor, Inbox }
import scala.concurrent.duration._

case object Greet
case class WhoToGreet(who: String)
case class Greeting(message: String)

class Greeter extends Actor {
  var greeting = ""

  def receive = {
    case WhoToGreet(who) => greeting = s"hello, $who"
    case Greet           => sender ! Greeting(greeting) // Send the current greeting back to the sender
    case _ => println(" Unknown Message")
  }
}

object HelloAkkaScala extends App {

  // Create the 'helloakka' actor system
  val system = ActorSystem("helloakka")

  // Create the 'greeter' actor
  val greeterRef = system.actorOf(Props[Greeter], "greeter")
  val greetPrinterRef = system.actorOf(Props[GreetPrinter])

  // Create an "actor-in-a-box"
  val inbox = Inbox.create(system)

  // Tell the 'greeter' to change its 'greeting' message
  greeterRef.tell(WhoToGreet("akka"), ActorRef.noSender)

  // Ask the 'greeter for the latest 'greeting'
  // Reply should go to the "actor-in-a-box"
  inbox.send(greeterRef, Greet)

  // Wait 5 seconds for the reply with the 'greeting' message
  val Greeting(message1) = inbox.receive(5.seconds)
  println(s"Greeting: $message1")

  // Change the greeting and ask for it again
  greeterRef.tell(WhoToGreet("typesafe"), ActorRef.noSender)
  inbox.send(greeterRef, Greet)
  val Greeting(message2) = inbox.receive(5.seconds)
  println(s"Greeting: $message2")

  greeterRef.tell(WhoToGreet("Imaya"), ActorRef.noSender)
  greeterRef.tell(Greet, greetPrinterRef)

  // after zero seconds, send a Greet message every second to the greeter with a sender of the greetPrinter
  //system.scheduler.schedule(0.seconds, 1.second, greeter, Greet)(system.dispatcher, greetPrinter)
  
}

// prints a greeting
class GreetPrinter extends Actor {
  def receive = {
    case Greeting(message) => println("Printing received message " + message)
  }
}