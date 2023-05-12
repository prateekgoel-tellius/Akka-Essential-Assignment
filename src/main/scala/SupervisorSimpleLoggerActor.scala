import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy
import scala.concurrent.duration._
import java.io.{File, FileWriter}
import java.time.LocalTime
import java.time.format.DateTimeFormatter
object SupervisorSimpleLoggerActor extends App{
  val system = ActorSystem("LoggerSystem")
  val currentTime = LocalTime.now()
  val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
  val formattedTime = currentTime.format(formatter)

  object LoggerActor {
    case class LogWarningMessage(msg: String) // for the logWarning file

    case class LogInfoMessage(msg: String) //for the logInfo file

    case class RollingFileAppender(newFilename: String) // for the filename change
  }

  class LoggerActor extends Actor {

    import LoggerActor._

    val warningFile = new File("warning.log")
    val infoFile = new File("info.log")

    override def receive: Receive = withWriters(new FileWriter(warningFile, true), new FileWriter(infoFile, true))

    private def withWriters(warningWriter: FileWriter, infoWriter: FileWriter): Receive = {
      case LogWarningMessage(msg) =>
        warningWriter.write(msg + "\n")
        context.become(withWriters(warningWriter, infoWriter))

      case LogInfoMessage(msg) =>
        infoWriter.write(msg + "\n")
        context.become(withWriters(warningWriter, infoWriter))

      case RollingFileAppender(newFilename) =>
        warningWriter.flush()
        infoWriter.flush()
        warningWriter.close()
        infoWriter.close()
        if (warningFile.exists())
          warningFile.renameTo(new File(newFilename + ".warning.log"))

        if (infoFile.exists())
          infoFile.renameTo(new File(newFilename + ".info.log"))

        context.become(withWriters(new FileWriter(warningFile, true), new FileWriter(infoFile, true)))
      case _ =>
        throw new RuntimeException("Not a valid case.")

    }
  }

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

  supervisor ! LogInfoMessage(s"[Info $formattedTime] : An info message")
  supervisor ! LogWarningMessage(s"[Warning $formattedTime] : A warning message")
  supervisor ! RollingFileAppender("logs/Prateek")
  supervisor ! "Hi"
  supervisor ! LogInfoMessage(s"[Info $formattedTime] : An info message")
  supervisor ! LogWarningMessage(s"[Warning $formattedTime] : A warning message")

  println("Logs Completed.")
  Thread.sleep(1000)
}
