package com.hacklanta
package lib

import java.io._
import java.util.concurrent.{BlockingQueue,LinkedBlockingQueue}

import scala.io._
import scala.sys.process._

import net.liftweb.actor.LAScheduler
import net.liftweb.common._

import rest.projectBaseDirectory

object SbtInteractor {
  val containerProjectDirectory = "/mnt/code"

  /**
   * Build an SbtInteractor and kicks off an sbt process; the returned
   * SbtInteractor won't be 100% ready to rock, but methods called on it
   * will buffer or block properly so that it can be used immediately.
   *
   * Returns Empty or Failure if the projectBaseDirectory isn't set up
   * properly in the session.
   */
  def apply(): Box[SbtInteractor] = {
    for {
      directory <- projectBaseDirectory.is
      outputQueue = new LinkedBlockingQueue[Option[String]]()
      sbtProcess = Process(
        "docker" :: "run" ::
          "-p" :: s"${dockerPort.is.toString}:8080" ::
          "-v" :: s"${directory.toString}:$containerProjectDirectory:rw" ::
          "-w" :: containerProjectDirectory ::
          "-i" ::
          "-t" :: "shadowfiend/lift-jungle-gym" ::
          "sbt" :: "-Dsbt.log.format=false" :: Nil,
        directory.toFile
      )

      protoInteractor = new SbtInteractor(outputQueue)
    } yield {
      sbtProcess.run(
        bidirectionalProcessIo(
          protoInteractor.setInputStream _,
          outputQueue
        )
      )

      protoInteractor
    }
  }

  private def bidirectionalProcessIo(setStdinStream: (PrintStream)=>Unit, outputLineQueue: BlockingQueue[Option[String]]) = {
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
      { stderrStream =>
        val reader = new BufferedReader(new InputStreamReader(stderrStream))

        Stream
          .continually(reader.readLine)
          .takeWhile(_ != null)
          .foreach(println _)
      }
    )
  }
}

/*
 * An object of this class allows interactions with an sbt
 * instance. This is mainly exposed through two methods:
 * runCommand(String) and output: Stream[String].
 */
class SbtInteractor(private var outputQueue: LinkedBlockingQueue[Option[String]]) {
  private var input: Option[PrintStream] = None
  private var pendingCommands = List[String]()

  /**
   * Provides the sbt output for this interactor as a {Stream[String]} that
   * blocks when output isn't available.
   */
  @volatile var output: Stream[Option[String]] = {
    // Without this, stream creation will block when it
    // tries to fetch the very first element in the stream.
    outputQueue.put(Some(""))

    Stream
      .continually(outputQueue.take)
      .takeWhile(_ != None)
  }

  LAScheduler.execute(() => {
    // Continuously replace the stream with the latest version; this is
    // so we don't cache earlier output. When a page connects, it will
    // only care about the output of sbt from that point forward.
    while (output != Stream.empty)
      output = output.tail
  })

  def setInputStream(inputStream: PrintStream) = {
    this.synchronized {
      input = Some(inputStream)

      pendingCommands.foreach { command =>
        inputStream.println(command)
        inputStream.flush
      }

      pendingCommands = Nil
    }
  }

  /**
   * Runs the given command in sbt, once any currently pending commands
   * are done running.
   */
  def runCommand(command: String) = {
    this.synchronized {
      input.map { input =>
        input.println(command)
        input.flush
      } getOrElse {
        pendingCommands ::= command
      }
    }
  }

  def stop() = {
    input.map(_.close)
  }
}
