package com.hacklanta
package lib

import scala.annotation.tailrec

import net.liftweb.http.SessionVar
import net.liftweb.util.Helpers.randomInt

object dockerPort extends SessionVar[Int](PortHelpers.randomAvailablePort)
object PortHelpers {
  var usedPorts = new java.util.concurrent.ConcurrentHashMap[Int,Any]

  private val AVAILABLE_PORTS = 48127
  private val PORT_OFFSET = 1024

  // TODO Evict ports on session expiration.

  @tailrec
  def randomAvailablePort: Int = {
    val randomPort = PORT_OFFSET + randomInt(AVAILABLE_PORTS) 

    if (usedPorts.contains(randomPort))
      randomAvailablePort
    else {
      usedPorts.put(randomPort, dockerPort)

      randomPort
    }
  }
}

