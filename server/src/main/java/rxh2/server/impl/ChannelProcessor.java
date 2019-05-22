package rxh2.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.reactivestreams.Processor;

/**
 * A channel processor processes the flow of {@link ByteBuf}s which are read from the
 * {@link Channel}, processes them and emits some {@link OutboundByteBuf}s to be written back
 * into the channel.
 */
public abstract class ChannelProcessor implements Processor<ByteBuf, OutboundByteBuf> {

  protected Channel channel;

  public ChannelProcessor(Channel channel) {
    this.channel = channel;
  }
}
