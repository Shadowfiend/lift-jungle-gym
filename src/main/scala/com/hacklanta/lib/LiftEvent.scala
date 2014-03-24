package com.hacklanta
package snippet

import net.liftweb._
  import http._
    import js._
      import JE._
  import json._
    import Extraction._

class LiftEvent(event:String, data:JObject = JObject(List())) extends JsCmd {
  override val toJsCmd = { 
    if (data.obj.length == 0)
      Call("liftAjax.event", event).cmd.toJsCmd
    else
      Call("liftAjax.event", event, data).cmd.toJsCmd
  }   
}
class BasicLiftEvent(event: String = null) extends JsCmd {
  implicit val formats = DefaultFormats

  private val eventName = Option(event) getOrElse BasicLiftEvent.eventNameFromObject(this)

  override val toJsCmd = {
    Call("liftAjax.event", eventName, decompose(this)).cmd.toJsCmd
  }
}
object BasicLiftEvent {
  def eventNameFromObject(eventObject: BasicLiftEvent) = {
    eventObject.getClass.getName
      .split('.')
      .last
      .replaceAll("([A-Z][a-z\\d]+)(?=([A-Z][a-z\\d]+))", "$1-")
      .toLowerCase
  }
}
