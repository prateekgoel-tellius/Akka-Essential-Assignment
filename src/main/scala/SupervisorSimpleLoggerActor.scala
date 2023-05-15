import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy
import scala.concurrent.duration._
import java.io.{File, FileWriter}
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import SimpleLoggerActor.LoggerActor
object SupervisorSimpleLoggerActor extends App{
  val system = ActorSystem("LoggerSystem")
  val currentTime = LocalTime.now()
  val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
  val formattedTime = currentTime.format(formatter)

  class Supervisor extends Actor {


    val loggerActor = context.actorOf(Props[LoggerActor], "LoggerActor")

    override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: RuntimeException => Resume
      case _: Exception => Restart
      case _ => Escalate
    }
    override def receive: Receive = {
      case msg => loggerActor.forward(msg)
    }
  }


  val supervisor = system.actorOf(Props[Supervisor], "supervisorActor")

  import LoggerActor._

  supervisor ! LogInfoMessage(s"[Info $formattedTime] : An info message1")
  supervisor ! LogWarningMessage(s"[Warning $formattedTime] : A warning message1")
  supervisor ! "Hi"
  supervisor ! LogInfoMessage(s"[Info $formattedTime] : An info message2")
  supervisor ! LogWarningMessage(s"[Warning $formattedTime] : A warning message2")
  supervisor ! RollingFileAppender("logs/Prateek")


  println("Logs Completed.")
  Thread.sleep(1000)
}
