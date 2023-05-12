import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class BankAccountSpec extends TestKit(ActorSystem("testSystem"))
  with AnyWordSpecLike with BeforeAndAfterAll with ImplicitSender {

  import BankActorSpec.BankAccount._
  import BankActorSpec.BankAccount
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
  object BankAccount {
    case class Deposit(amount: Int)

    case class Withdraw(amount: Int)

    case object Statement

    case class TransactionSuccess(message: String)

    case class TransactionFailure(message: String)
  }

  class BankAccount extends Actor with ActorLogging {

    import BankAccount._

    override def receive: Receive = accountReceive(0)

    def accountReceive(balance: Int): Receive = {
      case Deposit(amount) =>
        if (amount < 0) sender() ! TransactionFailure("invalid deposit amount")
        else {
          val newBalance = balance + amount
          context.become(accountReceive(newBalance))
          log.info(sender().toString())
          sender() ! TransactionSuccess(s"Successfully Deposited $amount, Total Balance : $newBalance")
        }

      case Withdraw(amount) =>
        if (amount < 0) sender() ! TransactionFailure("invalid withdraw amount")
        else if (amount > balance) sender() ! TransactionFailure("Withdrawal amount is bigger than balance")
        else {
          val newBalance = balance - amount
          context.become(accountReceive(newBalance))
          sender() ! TransactionSuccess(s"Successfully Withdrawal $amount, Total Balance: $newBalance")
        }

      case Statement => sender() ! s"Your balance is $balance"
    }
  }

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
