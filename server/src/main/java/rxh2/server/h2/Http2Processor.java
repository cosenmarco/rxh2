package rxh2.server.h2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.NonNull;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import rxh2.server.h2.frame.FrameDecoderImpl;
import rxh2.server.impl.ChannelProcessor;
import rxh2.server.impl.OutboundByteBuf;

public class Http2Processor extends ChannelProcessor {

  private Subscriber<? super OutboundByteBuf> subscriber;

  // Factory method to do dependency injection
  public static Http2Processor createFor(Channel channel) {
    return new Http2Processor(channel, new FrameDecoderImpl());
  }

  final FrameDecoderImpl frameDecoder;

  Http2Processor(@NonNull Channel channel, @NonNull FrameDecoderImpl frameDecoder) {
    super(channel);
    this.frameDecoder = frameDecoder;
    Flux.from(frameDecoder)
        .doOnError(System.err::println)
        .subscribe(System.out::println);
  }


  @Override
  public void onSubscribe(Subscription s) {
    // Nothing to do as upstream doesn't support backpressure
  }

  @Override
  public void onNext(ByteBuf byteBuf) {
    frameDecoder.onNext(byteBuf);
  }

  @Override
  public void onError(Throwable t) {
    frameDecoder.onError(t);
  }

  @Override
  public void onComplete() {
    frameDecoder.onComplete();
  }

  @Override
  public void subscribe(Subscriber<? super OutboundByteBuf> s) {
    subscriber = s;
  }
}
