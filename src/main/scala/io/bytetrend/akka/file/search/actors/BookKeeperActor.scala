package io.bytetrend.akka.file.search.actors

import java.nio.file.Path

import akka.actor.{Actor, ActorLogging, Props}
import io.bytetrend.akka.file.search.actors.BookKeeperActor.{ProduceReport, SearchResult}
import io.bytetrend.akka.file.search.actors.Coordinator.ProcessError

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt


object BookKeeperActor {
  val name = "BookKeeperActor"

  def props: Props = Props(classOf[BookKeeperActor])

  case class SearchResult(file: Path, results: Map[String, List[Long]], duration: Long)

  case class ProduceReport(totalFileCount: Int)

}

/**
 * This actor is responsible for receiving the results of all the file searches.
 */
class BookKeeperActor extends Actor with ActorLogging {

  private val results = ListBuffer[SearchResult]()

  override def receive: Receive = {

    case r: SearchResult =>
      log.debug(s"received message for file: ${r.file.getFileName} and result is ${r.results}")
      results += r
    case ProduceReport(totalFileCount) =>
      produceReport(totalFileCount)
    case ProcessError(path, exception, _) =>
      log.error(s"Error processing file $path reason: ${exception.getMessage}")
      results += SearchResult(path, Map.empty[String, List[Long]], 0)
    case m => log.warning(s"Unexpected exception message received $m")
  }

  /**
   * Prints all the files that were processed by the file search actor and
   * the messages received then one per line the token and the count found in all files.
   *
   * @param totalFileCount all files found by the coordinator actor.
   */
  def produceReport(totalFileCount: Int): Unit = {
    val msgToFiles = if (totalFileCount == results.size) "" else "does not"
    val tupleList = results.foldLeft(List[(String, Int)]()) { case (list, r) =>
      r.results.map(p => (p._1, p._2.size)).toList ::: list
    }
    val sum = foldLeftSum(tupleList)
    val buffer = sum.foldLeft(new StringBuffer()) { case (b, (token, count)) => b.append(s"token $token found $count times in files \n"); b }

    println(
      s"""
         |Total files found: $totalFileCount and $msgToFiles matches total message received ${results.size}
         | ${buffer.toString}
         |""".stripMargin)
    import scala.concurrent.ExecutionContext.Implicits.global
    context.system.scheduler.scheduleOnce(5 seconds) {
      context.system.terminate()
    }
  }

  /**
   * adds up all the instances of each token across the multiple files reports.
   * It produces a list of tuples where the first element is the token and the second is the count.
   * @param tuples list of all counts for each token in each file.
   * @tparam A a String
   * @return a list of token->count
   */
  private def foldLeftSum[A](tuples: List[(A, Int)]):List[(A,Int)] = {
    tuples.foldLeft(Map.empty[A, Int])({
      case (acc, (k, v)) => acc + (k -> (v + acc.getOrElse(k,0)))
    }).toList
  }


}