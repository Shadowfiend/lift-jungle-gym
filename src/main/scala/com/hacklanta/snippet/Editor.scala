package com.hacklanta
package snippet

import java.io._
import java.util.concurrent.{BlockingQueue,LinkedBlockingQueue}

import scala.io._
import scala.sys.process._

import net.liftweb.http._
import net.liftweb.util.Helpers._

import com.hacklanta.rest.projectBaseDirectory

object Editor {
  def render = {
    val file = Source.fromFile("/Users/Shadowfiend/github/lift-jungle-gym/src/main/scala/com/hacklanta/snippet/Editor.scala").getLines.mkString("\n")

    ".content *" #> file
  }

  def sbtRunner = {
    @volatile var stdinStream: Option[PrintStream] = None

    def bidirectionalProcessIo(setStdinStream: (PrintStream)=>Unit, outputLineQueue: BlockingQueue[Option[String]]) = {
      new ProcessIO(
        { runningStdinStream =>
          setStdinStream(new PrintStream(runningStdinStream))
        },
        { stdoutStream =>
          val reader = new BufferedReader(new InputStreamReader(stdoutStream))

          Stream
            .continually(reader.readLine)
            .takeWhile(_ != null)
            .foreach { line =>
              outputLineQueue.put(Some(line))
            }
          outputLineQueue.add(None)
        },
        _ => ()
      )
    }

    val runnerJs =
      for {
        session <- S.session
        directory <- projectBaseDirectory.is
        outputQueue = new LinkedBlockingQueue[Option[String]]()
        sbtProcess = Process("sbt" :: Nil, directory.toFile)
        roundtripJs =
          session.buildRoundtrip(List[RoundTripInfo](
            "startSbt" -> { _: String =>
              sbtProcess.run(
                bidirectionalProcessIo(
                  printStream => stdinStream = Some(printStream),
                  outputQueue
                )
              )

              Stream
                .continually(outputQueue.take)
                .takeWhile(_ != None)
            },
            "runSbtCommand" -> { command: String =>
              stdinStream.map { stream =>
                stream.println(command)
                stream.flush
              }
            },
            "stopSbt" -> { _: String =>
              stdinStream.map(_.close)
            }
          ))
      } yield {
        s"var sbtRunner = $roundtripJs"
      }

    "script *" #> runnerJs
  }

}
