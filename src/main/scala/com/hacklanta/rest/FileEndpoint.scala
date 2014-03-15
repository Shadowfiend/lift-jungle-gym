package com.hacklanta
package rest

import java.nio.file._

import scala.collection.JavaConversions._
import scala.sys.process._

import net.liftweb.common._
import net.liftweb.http._
  import rest._
  import LiftRules._
import net.liftweb.util.Helpers._

object projectBaseDirectory extends SessionVar[Box[Path]](FileEndpoint.setUpTempDirectory)
object fileEndpointDirectory extends SessionVar[Box[Path]](projectBaseDirectory.is.map(_.resolve("src/main/scala/code")))
object bootEndpointDirectory extends SessionVar[Box[Path]](projectBaseDirectory.is.map(_.resolve("src/main/scala/bootstrap/liftweb")))
object FileEndpoint extends RestHelper {
  val apiPrefix = "source"

  val githubRepository = ("Shadowfiend", "knock-me-out-lift-example")

  def apiPrefixProcessor: DataAttributeProcessor = {
    case ("lift-file-api-prefix", _, element, _) =>
      element % ("data-file-api-prefix" -> s"/$apiPrefix/")
  }

  def setUpTempDirectory = {
    for {
      session <- S.session ?~ "No session to associate with temp directory."
      (githubUser, repositoryName) = githubRepository
      directory <- tryo(Files.createTempDirectory(session.uniqueId))
      // hook up docker here to do a git clone instead
      _ = Process(
        "git" :: "clone" :: s"git://github.com/$githubUser/$repositoryName.git" :: Nil,
        directory.toFile
      ).!
    } yield {
      directory.resolve(repositoryName).toRealPath()
    }
  }

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
          bootEndpointDirectory.is.map(_.resolve("Boot.scala"))
            .filter(Files.isRegularFile(_))

        case directory :: file :: Nil if directoryWhitelist.contains(directory) =>
          fileEndpointDirectory.is.map(Files.newDirectoryStream _).flatMap { baseDirectory =>
            baseDirectory
              .find(_.getFileName.toString == directory)
              .flatMap { matchingDirectory =>
                Files.newDirectoryStream(matchingDirectory)
                  .find(_.getFileName.toString == file)
                  .filter(Files.isRegularFile(_))
              }
          }

        case _ =>
          None
      }
    }
  }

  serve {
    case `apiPrefix` :: SafePath(path) Get _ =>
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

    case `apiPrefix` :: SafePath(path) Post req =>
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
