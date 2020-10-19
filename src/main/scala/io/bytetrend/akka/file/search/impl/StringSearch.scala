package io.bytetrend.akka.file.search.impl

/**
 * This represents what a search implementation should
 * have. Implementations should take the string to search and
 * process the file in chunks while keeping the state of the search.
 *
 * @tparam T the underlying implementation of the file reader's buffer.
 */
trait StringSearch[T] {

  /**
   * Search is performs in chunks of the file.
   * @param chunk piece of characters from the file to search.
   * @param charsInChunk amount of characters in the chunk that are valid.
   * @param charsSoFar position in the file of the last character in the chunk.
   */
  def search(chunk:T,charsInChunk:Int,charsSoFar:Long)

  /**
   * Returns the results when the search hits the end of the file.
   * @return A list of longs that represent the starting location
   *         in the file of the string being searched
   */
  def results:List[Long]

  def token:String

}
