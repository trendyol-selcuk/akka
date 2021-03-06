/**
 * Copyright (C) 2016-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.remote.artery

import scala.concurrent.duration._

import akka.actor._
import akka.testkit.ImplicitSender
import akka.testkit.SocketUtil
import akka.testkit.TestActors
import com.typesafe.config.ConfigFactory

object HandshakeRetrySpec {

  // need the port before systemB is started
  val portB = SocketUtil.temporaryLocalPort(udp = true)

  val commonConfig = ConfigFactory.parseString(s"""
     akka.remote.artery.advanced.handshake-timeout = 10s
     akka.remote.artery.advanced.image-liveness-timeout = 7s
  """).withFallback(ArterySpecSupport.defaultConfig)

}

class HandshakeRetrySpec extends ArteryMultiNodeSpec(HandshakeRetrySpec.commonConfig) with ImplicitSender {
  import HandshakeRetrySpec._

  "Artery handshake" must {

    "be retried during handshake-timeout (no message loss)" in {
      def sel = system.actorSelection(s"akka://systemB@localhost:$portB/user/echo")
      sel ! "hello"
      expectNoMsg(1.second)

      val systemB = newRemoteSystem(
        name = Some("systemB"),
        extraConfig = Some(s"akka.remote.artery.canonical.port = $portB")
      )
      systemB.actorOf(TestActors.echoActorProps, "echo")

      expectMsg("hello")

      sel ! Identify(None)
      val remoteRef = expectMsgType[ActorIdentity].ref.get

      remoteRef ! "ping"
      expectMsg("ping")
    }

  }

}
