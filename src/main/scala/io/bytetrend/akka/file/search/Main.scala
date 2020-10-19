package io.bytetrend.akka.file.search

import java.nio.file.Paths

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.bytetrend.akka.file.search.Constants.systemName
import io.bytetrend.akka.file.search.actors.Coordinator
import io.bytetrend.akka.file.search.actors.Coordinator.SearchDirectory

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

/**
 * This is the actor system main entry point. The configuration for the
 * system is defined in the application.conf. This bootstrap starts the main actor
 * which is the coordinator. In turn the coordinator starts the other actors and
 * recursively searches for files and directories to search.
 */
object Main extends App {
  val logger = Logger.apply(Main.getClass)
  val system = ActorSystem(systemName, ConfigFactory.load().getConfig(systemName))
  val coordinator = system.actorOf(Coordinator.props, Coordinator.name)
  val settings = Settings


  //Add hook for graceful shutdown
  sys.addShutdownHook {
    system.log.info("Shutting down")
    system.terminate()
    Await.result(system.whenTerminated, settings.serviceTimeout.duration)
    logger.debug(s"Actor system ${system.name} successfully shut down")
  }

  //dispatcher is needed for call to scheduler.

  import system.dispatcher

  system.scheduler.scheduleOnce(500 millis) {
    Try {
      //This is how the search is primed. Either from the command line passing a directory path
      //Or adding a directory to search in application.conf
      if (args.length == 0) Paths.get(Settings.searchDir) else Paths.get(args(0))
    } match {
      case Success(path) => coordinator ! SearchDirectory(path, Settings.searchFileTokens.asScala, Some(Settings.searchFileTypes.asScala))
      case Failure(e) =>
        logger.error(s"Invalid path to search: ${e.getMessage} exiting now.")
        system.terminate()
    }
  }

}

