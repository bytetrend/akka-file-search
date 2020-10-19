package io.bytetrend.akka.file.search.impl

import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer

class ByteBufferSearch(searchString:String) extends StringSearch[ByteBuffer] {

  private[this] val positions = ArrayBuffer[Long]()
  /**
   * Last position in the file processed by last search.
   */
  private[this] var prevLastPosition = 0L

  /**
   * Search is performs in chunks of the file.
   *
   * @param chunk     piece of characters from the file to search.
   * @param charsInChunk amount of characters in the chunk that are valid.
   * @param charsSoFar  position in the file of the last character in the chunk.
   */
  override def search(chunk: ByteBuffer, charsInChunk: Int, charsSoFar: Long): Unit = {
    var indexInString = 0
    for(c <- 0 until charsInChunk){
      if(chunk.get(c) == searchString.charAt(indexInString))
        indexInString += 1
      else
        indexInString = 0
      if(indexInString == searchString.length){
        //Add to the list of positions the sum of the last position in the file
        // plus the current position in the chunk which is the current character index
        //minus the length of the string being searched.
        positions += prevLastPosition + (c - searchString.length)
        indexInString = 0
      }
      prevLastPosition = charsSoFar
    }

  }

  /**
   * Returns the results when the search hits the end of the file.
   *
   * @return A list of longs that represent the starting location
   *         in the file of the string being searched
   */
  override def results: List[Long] = positions.toList

  override def token: String = searchString
}
