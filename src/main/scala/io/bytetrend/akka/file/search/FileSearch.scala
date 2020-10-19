package io.bytetrend.akka.file.search

import io.bytetrend.akka.file.search.actors.BookKeeperActor.SearchResult
import io.bytetrend.akka.file.search.actors.Coordinator.SearchRequest

trait FileSearch {

  def search(req: SearchRequest): Either[SearchResult, Exception]

}
