package io.bytetrend.akka.file.search

import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import io.bytetrend.akka.file.search.Constants.systemName

import scala.concurrent.duration.FiniteDuration

/**
 * Utility class that represent the configurations for the system.
 * This includes all Akka configurations.
 * It has convenient methods to convert and access any configuration.
 * @param config
 */
class Settings(val config: Config) {
  private [this] val fileSearchConfig = config.getConfig(systemName)


  val searchDir = fileSearchConfig.getString("search-dir")
  val searchWords = fileSearchConfig.getStringList("search-words")
  val searchFileTypes = fileSearchConfig.getStringList("search-file-types")
  val searchFileTokens = fileSearchConfig.getStringList("search-file-tokens")
  val serviceTimeout = Timeout(getFiniteDuration("service-timeout"))
  val routerPoolSize = fileSearchConfig.getInt("search-pool-size")
  val knuttMorrisPratBufferSize = fileSearchConfig.getInt("knuttMorrisPratt-buffer-size")


  def getFiniteDuration(path: String): FiniteDuration = {
    import scala.concurrent.duration._
    fileSearchConfig.getDuration(path, java.util.concurrent.TimeUnit.MILLISECONDS).millis
  }

  def getSearchAlgo:FileSearch = {
    Class.forName(fileSearchConfig.getString("search-algo")).newInstance().asInstanceOf[FileSearch]
  }
}

object Settings extends Settings(ConfigFactory.load("application.conf"))