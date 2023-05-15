import SupervisorSimpleLoggerActor.LoggerActor
import akka.actor.ActorSystem

import java.io.{File, FileWriter}
import akka.actor.{Actor, Props}

import java.time.LocalTime
import java.time.format.DateTimeFormatter

object SimpleLoggerActor extends App {
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

  val loggerActor = system.actorOf(Props[LoggerActor],"LoggerActor")
  import LoggerActor._
  loggerActor ! LogInfoMessage(s"[Info $formattedTime] : An info message")
  loggerActor ! LogWarningMessage(s"[Warning $formattedTime] : A warning message")

  loggerActor ! RollingFileAppender("logs/Prateek")
  println("Logs Completed.")
  Thread.sleep(1000)
}

