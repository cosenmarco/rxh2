package rxh2.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.reactivex.Observable;
import io.reactivex.Observer;

/**
 * A channel processor processes the flow of {@link ByteBuf}s which are read from the
 * {@link Channel}, processes them and emits some {@link ByteBuf}s to be written back into the
 * channel.
 */
public abstract class ChannelProcessor extends Observable<ByteBuf> implements Observer<ByteBuf> {

  protected Channel channel;

  public ChannelProcessor(Channel channel) {
    this.channel = channel;
  }
}
