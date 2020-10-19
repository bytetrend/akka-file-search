# akka-file-search: It is an example of a file search for tokens the uses the Knuth–Morris–Pratt algorithm and Akka.

**akka-file-search** This is a example of implementing an Akka system to recursively search for tokens in files. The implemended algorithm at this time is [knuth-morris-pratt]). The project uses Scala version 2.12 and Akka version 2.6.10 which as of October 2020 are the latest versions. 
## Project Architecture

The project is composed of 3 actors, a coordinator, a bookkepper, and the file search. The coordinator is the parent supervisor. It creates the two other actors and acts as their supevisor. Its supervisory strategy is "OneForOneStrategy". It is bootstrap from the main rutine by sending a message with the starting path from where to search for files. It then recursively searches directories for files that have a given set of extensions. For each file found it sends a message to the "FileSearchActor". The file search actor is setup with the given file search strategy by reading the configuration from an "application.conf". It then reads the file in chunks so as to limit the amount of memory consumed and this was being able to process very large files. For each token that is configured to search for it maintains a list of where in the file that token was found. At the end it send a message to the bookkepper with those results. 
The bookkeeper actors receives search result messages and stores those. Once all the files under that directory have been processes the coordinator sends a message to the bookkepper to produce its result. Once the result is produce the process exits.
There is no doubt that Akka is a very high throughtput and scalable technology. We are able to search multiple files in parallel by implementing the search using the Akka Routing pattern. For that we use a round-robin-pool with a configurable size.

## Summary
The design of the search was done using some of the most efficient technologies available in Java 8. In this case Java NIO was used in the search mainly two components: FileChannel and DirectoryStream. Those components use memory more efficiently by letting the OS do most of the work and avoiding loading the whole file in memory. It shows that even though there are very large files under the windows directory the memory usage was under 0.5 GB. The other reason for this is the search strategy implemented was an efficient one: Knuth-Morris-Pratt. This prevented loading the whole file in memory and just using an stream of up to 1024 bytes to process each file. A total of 150K files were searched with a combined size of 31GB a the memory stayed under 0.5 GB.

[knuth-morris-pratt]: https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm