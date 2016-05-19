package com.github.scalalab3.logs

import java.net.InetSocketAddress
import scala.util.{Try, Success, Failure}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import play.api.libs.json.{Json, JsValue}

import common.Log


trait BaseListener extends Actor with ActorLogging {
  val output: ActorRef

  def writeLog(msg:Log) = output ! msg
}


class LineReceiver (out:ActorRef) extends BaseListener {
  import Tcp._
  import common_macro._

  val output = out

  def receive = {
    case Received(data) => {
      log.info(s"Received: [${data.utf8String}]")
      parseLine(data.utf8String)
    }
    case PeerClosed => context stop self
  }

  def parseLine(line:String) = Try(Json.parse(line)) match {
    case Success(jsonLog) => materialize[Log](jsonLog).foreach(writeLog _)
    case Failure(_) =>
  }
}

class TCPListener(host:String, port:Int, out:ActorRef) extends Actor with ActorLogging {
  import context.system
  import Tcp._

  log.info(s"Bind to ${host}:${port}")
  IO(Tcp) ! Bind(self, new InetSocketAddress(host, port))

  def receive = {
    case bound@Bound(localAddress) => {
      log.info(s"Bound: bound")
      out ! "ready"
    }
    case connected@Connected(remote, local) => {
      val handler = context.actorOf(Props(classOf[LineReceiver], out))
      val conn = sender()
      conn ! Register(handler)
    }
    case CommandFailed(msg: Bind) => {
      log.warning(s"Bind failed: $msg")
      context stop self
    }
    case msg => log.info(s"Got other msg: $msg")
  }
}
