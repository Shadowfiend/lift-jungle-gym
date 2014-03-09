package com.hacklanta
package snippet

import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.util.Helpers._

import lib.SbtInteractor

object interactor extends SessionVar[Box[SbtInteractor]](SbtInteractor())
object Editor {
  def sbtRunner = {
    val runnerJs =
      for {
        session <- S.session
        interactor <- interactor.is
        roundtripJs =
          session.buildRoundtrip(List[RoundTripInfo](
            "connect" -> { _: String => interactor.output },
            "runCommand" -> interactor.runCommand _,
            "stop" -> { _: String => interactor.stop() }
          ))
      } yield {
        s"var sbtRunner = $roundtripJs"
      }

    "script *" #> runnerJs
  }

}
