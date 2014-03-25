package com.hacklanta
package snippet

import java.nio.file._

import scala.collection.JavaConversions._

import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.util._
  import Helpers._

import lib.SbtInteractor
import com.hacklanta.rest._

object interactor extends SessionVar[Box[SbtInteractor]](SbtInteractor())
object Editor {
  def availableSnippets = {
    ClearClearable andThen
    "li" #> {
      for {
        packageDirectory <- fileEndpointDirectory.is.toList
        snippetDirectory = packageDirectory.resolve("snippet")
        directoryStream = Files.newDirectoryStream(snippetDirectory)
        entry <- directoryStream
          if Files.isRegularFile(entry)
        filename = entry.getFileName.toString
      } yield {
        "a *" #> filename &
        "a [href]" #> s"snippet/$filename"
      }
    }
  }

  private def containerEndpointFor(absoluteProjectEndpoint: Box[Path]): Stream[String] = {
    for {
      projectBaseDirectory <- projectBaseDirectory.is.toStream
      absoluteProjectEndpoint <- absoluteProjectEndpoint.toStream
      relativeProjectEndpoint = projectBaseDirectory.relativize(absoluteProjectEndpoint)
    } yield {
      s"${SbtInteractor.containerProjectDirectory}/$relativeProjectEndpoint/"
    }
  }

  def sbtRunner = {
    val runnerJs =
      for {
        session <- S.session
        interactor <- interactor.is
        roundtripJs =
          session.buildRoundtrip(List[RoundTripInfo](
            "connect" -> { _: String =>
              for {
                fileStringToStrip <- containerEndpointFor(fileEndpointDirectory.is)
                bootStringToStrip <- containerEndpointFor(bootEndpointDirectory.is)
                possibleLine <- interactor.output
                line <- possibleLine
              } yield {
                line
                  .replace(fileStringToStrip, "")
                  .replace(bootStringToStrip, "")
              }
            },
            "runCommand" -> interactor.runCommand _,
            "stop" -> { _: String => interactor.stop() }
          ))
      } yield {
        s"var sbtRunner = $roundtripJs"
      }

    "script *" #> runnerJs
  }

}
