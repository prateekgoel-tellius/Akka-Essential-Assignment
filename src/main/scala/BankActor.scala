
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

object BankActor extends App {

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

  class Person extends Actor {
    import Person._
    import BankAccount._

    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(10000)
        account ! Withdraw(50000)
        account ! Withdraw(5000)
        account ! Statement

      case message => println(message.toString)
    }
  }

  import Person._
  val system = ActorSystem("BankActor")

  val Account1 = system.actorOf(Props[BankAccount], "Account1")
  val Person1 = system.actorOf(Props[Person], "Person1")

  Person1 ! LiveTheLife(Account1)
}
