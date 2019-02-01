package org.edla.netty.example.echo

import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Logger

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelUpstreamHandler

/**
  * Handler implementation for the echo client.  It initiates the ping-pong
  * traffic between the echo client and server by sending the first message to
  * the server.
  */
class EchoClientHandler(firstMessageSize: Int) extends SimpleChannelUpstreamHandler {

  require(firstMessageSize > 0)

  private val logger = Logger.getLogger(getClass.getName)

  private val transferredBytes = new AtomicLong

  private val firstMessage = ChannelBuffers.buffer(firstMessageSize)
  val range: Range         = 0.until(firstMessage.capacity)
  for (i ← range) { firstMessage.writeByte(i.toByte) }

  def getTransferredBytes: Long = transferredBytes.get

  // Send the first message.  Server will not send anything here
  // because the firstMessage's capacity is 0.
  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
    e.getChannel.write(firstMessage)
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
    // Send back the received message to the remote peer.
    transferredBytes.addAndGet((e.getMessage match {
      case c: ChannelBuffer ⇒ c
      case _                ⇒ throw new ClassCastException
    }) readableBytes)
    e.getChannel.write(e.getMessage)
  }

  override def exceptionCaught(context: ChannelHandlerContext, e: ExceptionEvent): Unit = {
    // Close the connection when an exception is raised.
    logger.warning("Unexpected exception from downstream." + e.getCause)
    e.getChannel.close()
  }
}
