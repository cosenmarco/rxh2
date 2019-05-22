package rxh2.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

/**
 * A Factory for {@link ChannelProcessor}s.
 */
@FunctionalInterface
public interface ChannelProcessorFactory {

  /**
   * @param nettyChannel the channel for which we want to build the processor
   * @return a non-null {@link ChannelProcessor} which will process the incoming {@link ByteBuf}
   * from the {@link Channel} and can be subscribed to in order to receive {@link ByteBuf}s to write
   * back into the channel
   */
  ChannelProcessor buildProcessor(Channel nettyChannel);
}
