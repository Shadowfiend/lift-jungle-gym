package com.hacklanta
package snippet

import scala.annotation.tailrec

import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.util.Helpers._

import lib._

case class PreviewLaunched(previewHost: String) extends BasicLiftEvent
case class PreviewLaunchFailed(error: String) extends BasicLiftEvent

object Previewer {
  private val hostPattern = "app-%s.jungle-gym.hacklanta.com"

  def previewHost = {
    hostPattern format dockerPort.is
  }

  def launchPreview = {
    {
      for {
        interactor <- interactor.is
        outputStream = interactor.output
      } yield {
        interactor.runCommand("container:stop")
        interactor.runCommand("container:start")

        // Wait for two successes to arrive, since the first one is a
        // successful stop and the second a successful start. We also
        // drop the first entry, which is likely the success of the last
        // command.
        outputStream
          .drop(1)
          .dropWhile(_.map(! _.startsWith("[success]")) getOrElse false)
          .drop(1)
          .dropWhile(_.map(! _.startsWith("[success]")) getOrElse false)

        PreviewLaunched(previewHost)
      }
    } match {
      case Full(cmd) => cmd
      case Failure(message, _, _) =>
        PreviewLaunchFailed(message)
      case _ =>
        PreviewLaunchFailed("Unknown error :/")
    }
  }

  def preview = {
    ".test [onclick]" #> SHtml.onEvent(_ => launchPreview)
  }
}
