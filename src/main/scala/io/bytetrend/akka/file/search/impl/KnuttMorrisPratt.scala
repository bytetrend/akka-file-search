package io.bytetrend.akka.file.search.impl

import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

import io.bytetrend.akka.file.search.actors.BookKeeperActor.SearchResult
import io.bytetrend.akka.file.search.actors.Coordinator.SearchRequest
import io.bytetrend.akka.file.search.{FileSearch, Settings}

import scala.util.{Failure, Success, Try}

/**
 * This class implements the Knuth–Morris–Pratt string-searching
 * algorithm (or KMP algorithm) searches for occurrences of a
 * "word" W within a main "text string" S by employing the
 * observation that when a mismatch occurs, the word itself
 * embodies sufficient information to determine where the
 * next match could begin, thus bypassing re-examination
 * of previously matched characters.
 * This is a linear-time algorithm for string matching.
 */
class KnuthMorrisPratt extends FileSearch {

  override def search(req: SearchRequest): Either[SearchResult, Exception] = {

    val buffer = ByteBuffer.allocate(Settings.knuttMorrisPratBufferSize)
    val searchers: Array[ByteBufferSearch] = req.searchTokens.zipWithIndex
      .foldLeft(new Array[ByteBufferSearch](req.searchTokens.length))((arr, x) => {
        arr(x._2) = new ByteBufferSearch(x._1)
        arr
      })
    Try {
      val reader: FileChannel = new FileInputStream(req.file.toFile).getChannel
      var numCharsRead = reader.read(buffer)
      while (numCharsRead > 0) {
        for (s <- searchers) {
          s.search(buffer, numCharsRead, reader.position)
        }
        numCharsRead = reader.read(buffer)
      }
    } match {
      case Success(_) => {
        val r = searchers.foldLeft(Map[String, List[Long]]())((acc, x) => {
          acc + (x.token -> x.results)
        })
        Left(SearchResult(req.file, r, 0L))

      }
      case Failure(exception) => {
        Right(new Exception(s"Error processing file ${req.file}",exception))
      }
    }
  }
}
