package io.bytetrend.akka.file.search.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.Materializer
import io.bytetrend.akka.file.search.Settings
import io.bytetrend.akka.file.search.actors.Coordinator.{ProcessError, SearchRequest}

object FileSearchActor {
  val name = "FileSearchActor"

  def props: Props = Props(classOf[FileSearchActor])
}

/**
 * This actor has the responsibility of perform token search on a given file.
 * The result of the search is sent as a message to actor "bookKeeperActor"
 * who is responsible to produce a report.
 *
 */
class FileSearchActor extends Actor with ActorLogging {
  final implicit val materializes: Materializer = Materializer.createMaterializer(context.system)
  private[this] val searchAlgo = Settings.getSearchAlgo

  private[this] val bookKeeperActor = context.actorOf(BookKeeperActor.props, BookKeeperActor.name)

  override def receive: Receive = {
    /**
     * For each search request run the configured algo and search file
     * for tokens in request. Then send search result as a message to bookkeeper.
     */
    case req: SearchRequest => {
      searchAlgo.search(req) match {
        case Left(result) =>
          sender ! result
        case Right(exception) =>
          sender ! ProcessError(req.file, exception, System.currentTimeMillis())
      }

    }

    case m => log.warning(s"Unexpected exception message received $m")
  }
}
