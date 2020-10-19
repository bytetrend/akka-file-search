package io.bytetrend.akka.file.search.actors

import java.io.File
import java.nio.file.{Files, Path}

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.routing.RoundRobinPool
import io.bytetrend.akka.file.search.Settings
import io.bytetrend.akka.file.search.actors.BookKeeperActor.{ProduceReport, SearchResult}
import io.bytetrend.akka.file.search.actors.Coordinator.{ProcessError, SearchDirectory, SearchRequest}

import scala.util.{Failure, Success, Try}

object Coordinator {

  val name = "SystemCoordinator"

  def props: Props = Props(classOf[Coordinator])

  case class SearchDirectory(directory: Path, searchTokens: Seq[String], fileType: Option[Seq[String]])

  case class SearchRequest(file: Path, searchTokens: Seq[String])

  case class UnAccessible(path: Path)

  case class ProcessError(path: Path, cause: Throwable, timeUTC: Long)
    extends RuntimeException(s"Error processing file ${path.getFileName}", cause)

}

/**
 * This actor is the parent of all the other actors. As such it implements
 * the strategy OneForOneStrategy to restart any failed child.
 * This Actor is prime with an initial message of a directory to search.
 *
 */
class Coordinator extends Actor with ActorLogging {

  override val supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy() {

      case e: Exception =>
        log.warning(s"Supervisor actor event received will resume actor due to: $e")
        Resume
    }
  val workerActors: ActorRef = context.actorOf(Props[FileSearchActor].withRouter(RoundRobinPool(Settings.routerPoolSize)), name = "WorkerActors")

  private[this] val bookKeeperActor = context.actorOf(BookKeeperActor.props, BookKeeperActor.name)
  private var totalFileCount = 0
  private var fileProcessed = 0

  /**
   * Receives an starting message with a path of the root directory to start looking for
   * files to search.
   *
   * @return
   */
  override def receive: Receive = {

    case SearchDirectory(path, tokens, fileTypes) =>
      Try {
        searchDirectory(path, tokens, fileTypes)
      } recover {
        case e: Exception =>
          log.error(s"coordinator could not process search request exiting now. error: $e")
          context.system.terminate()
      }
    case r: SearchResult =>
      log.debug(s"received message for file: ${r.file.getFileName} and result is ${r.results}")
      fileProcessed += 1
      bookKeeperActor ! r
      sendProduceReport()
    case ProcessError(path, exception, t) =>
      log.error(s"Error processing file $path reason: ${exception.getMessage}")
      fileProcessed += 1
      bookKeeperActor ! ProcessError(path, exception, t)
    case m => log.warning(s"Unexpected message received $m")
  }

  def sendProduceReport(): Unit = {
    if (fileProcessed == totalFileCount)
      bookKeeperActor ! ProduceReport(totalFileCount)
  }

  def searchDirectory(path: Path, tokens: Seq[String], fileTypes: Option[Seq[String]]) {

    val stream = Files.newDirectoryStream(path)
    import scala.collection.JavaConverters._
    for (entry <- stream.asScala) {
      Try {
        val file = entry.toFile
        if (!file.canRead)
          log.warning(s"Can't access file: $entry")
        if (file.isDirectory)
          self ! SearchDirectory(entry, tokens, fileTypes)
        else {
          if (!filter(file, fileTypes)) {
            workerActors ! SearchRequest(entry, tokens)
            totalFileCount += 1
          }
        }
        entry
      } match {
        case Success(entry) => log.debug(s"processed ${entry.getFileName}")
        case Failure(i: Throwable) => self ! ProcessError(path, i, System.currentTimeMillis)
      }
    }
  }

  /**
   * This method will return true if the file represented by path should be skip
   * otherwise false.
   *
   * @param file       file to be tested to see if of given extensions
   * @param extensions an option of extensions names.
   * @return true to skip the file else false.
   */
  def filter(file: File, extensions: Option[Seq[String]]): Boolean = {
    val result = extensions match {
      case None => false
      case Some(names) => names.foldLeft(true)((filter, ext) => {
        if (filter && file.toString.endsWith(ext))
          false
        else
          filter
      })
    }
    result
  }

}