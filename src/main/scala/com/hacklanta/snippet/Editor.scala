package com.hacklanta
package snippet

import io._

import net.liftweb.util.Helpers._

object Editor {
  def render = {
    val file = Source.fromFile("/Users/Shadowfiend/github/lift-jungle-gym/src/main/scala/com/hacklanta/snippet/Editor.scala").getLines.mkString("\n")

    ".content *" #> file
  }
}
