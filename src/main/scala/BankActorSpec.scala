import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random
import BankActor.BankAccount
import BankAccount._

class BankAccountSpec extends TestKit(ActorSystem("testSystem"))
  with AnyWordSpecLike with BeforeAndAfterAll with ImplicitSender {

  import BankActorSpec.Person
  import BankActorSpec.Person._

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Person actor" should {

    "deposit, withdraw and return balance correctly" in {
      val bankAccount = system.actorOf(Props[BankAccount], "bankaccount")
      val person = system.actorOf(Props(new Person(testActor)), "person")

      person ! LiveTheLife(bankAccount)
      expectMsg(TransactionSuccess("Successfully Deposited 10000, Total Balance : 10000"))
      expectMsg(TransactionFailure("Withdrawal amount is bigger than balance"))
      expectMsg(TransactionSuccess("Successfully Withdrawal 5000, Total Balance: 5000"))
      expectMsg("Your balance is 5000")
    }
  }
  "Depositing money" should{
    "be added" in {
      val bankAccount = system.actorOf(Props[BankAccount])
      bankAccount ! Deposit(10000)
      expectMsg(TransactionSuccess("Successfully Deposited 10000, Total Balance : 10000"))
    }
  }
  "Withdrawing money" should {
    "from account" in {
      val bankAccount = system.actorOf(Props[BankAccount])
      bankAccount ! Deposit(10000)
      expectMsg(TransactionSuccess("Successfully Deposited 10000, Total Balance : 10000"))
      bankAccount ! Withdraw(5000)
      expectMsg(TransactionSuccess("Successfully Withdrawal 5000, Total Balance: 5000"))
    }
  }
  "Withdrawing over money" should {
    "from account" in {
      val bankAccount = system.actorOf(Props[BankAccount])
      bankAccount ! Deposit(10000)
      expectMsg(TransactionSuccess("Successfully Deposited 10000, Total Balance : 10000"))
      bankAccount ! Withdraw(50000)
      expectMsg(TransactionFailure("Withdrawal amount is bigger than balance"))
    }
  }

}
object BankActorSpec {
  object Person {
    case class LiveTheLife(account: ActorRef)
  }

  class Person(actor1: ActorRef) extends Actor with ActorLogging{

    import Person._
    import BankAccount._

    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(10000)
        account ! Withdraw(50000)
        account ! Withdraw(5000)
        account ! Statement

      case message =>
        actor1 ! message
    }
  }

}
