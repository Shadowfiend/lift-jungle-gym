package com.hacklanta
package rest

import java.nio.file._

import scala.collection.JavaConversions._

import net.liftweb.http._
  import rest._
import net.liftweb.util.Helpers._

object FileEndpoint extends RestHelper {
  lazy val baseDirectory = Paths.get("/Users/Shadowfiend/github/lift-jungle-gym/src/main/scala/com/hacklanta")

  val directoryWhitelist =
    Set(
      "snippet",
      "model",
      "actor",
      "comet"
    )

  /**
   * An extractor that ensures the provided path:
   * (1) consists of only one directory and one file,
   * (2) ensures the directory is in the directory whitelist, and
   * (3) ensures the directory and file are direct children
   *     of the base directory
   *
   * There is also special-case handling for the Boot.scala file, whose
   * path is hard-coded.
   */
  object SafePath {
    def unapply(path: List[String]): Option[Path] = {
      path match {
        case "Boot.scala" :: Nil =>
          Some(baseDirectory.resolve("../../bootstrap/liftweb/Boot.scala"))
            .filter(Files.isRegularFile(_))

        case directory :: file :: Nil if directoryWhitelist.contains(directory) =>
          Files.newDirectoryStream(baseDirectory)
            .find(_.getFileName.toString == directory)
            .flatMap { matchingDirectory =>
              Files.newDirectoryStream(matchingDirectory)
                .find(_.getFileName.toString == file)
                .filter(Files.isRegularFile(_))
            }

        case _ =>
          None
      }
    }
  }


  serve {
    case "source" :: SafePath(path) Get _ =>
      tryo {
        StreamingResponse(
          data = Files.newInputStream(path),
          onEnd = ()=>(),
          size = Files.size(path),
          headers = List("Content-Type" -> "text/plain"),
          cookies = Nil,
          code = 200
        )
      }

    case "source" :: SafePath(path) Post req =>
      req.rawInputStream.flatMap { inputStream =>
        tryo {
          val outputStream = Files.newOutputStream(path)

          val thing = scala.collection.mutable.ArrayBuffer[Byte]()

          Iterator
            .continually(inputStream.read)
            .takeWhile(_ != -1)
            .foreach { stuff =>
              outputStream.write(stuff)
            }

          inputStream.close
          outputStream.close

          OkResponse()
        }
      }
  }
}
