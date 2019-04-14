package rxh2.server.h2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import lombok.NonNull;
import rxh2.server.h2.frame.FrameDecoderImpl;
import rxh2.server.impl.ChannelProcessor;

public class Http2Processor extends ChannelProcessor {
  private final static int CONNECTION_PREFACE_LENGTH = 24;
  private final static ByteBuf EXPECTED_PREFACE = Unpooled.wrappedBuffer(new byte[]{
      0x50, 0x52, 0x49, 0x20, 0x2a, 0x20, 0x48, 0x54, 0x54, 0x50, 0x2f, 0x32, 0x2e,
      0x30, 0x0d, 0x0a, 0x0d, 0x0a, 0x53, 0x4d, 0x0d, 0x0a, 0x0d, 0x0a
  });

  final FrameDecoder frameDecoder;
  private Observer<? super ByteBuf> observer;
  private boolean handshaked = false;
  private ByteBuf handshakeBuffer = null;

  Http2Processor(@NonNull Channel channel, @NonNull FrameDecoder frameDecoder) {
    super(channel);
    this.frameDecoder = frameDecoder;
  }

  @Override
  protected void subscribeActual(Observer<? super ByteBuf> observer) {
    this.observer = observer;
  }

  @Override
  public void onSubscribe(Disposable d) {

  }

  @Override
  public void onNext(ByteBuf byteBuf) {
    if (handshaked) {
      frameDecoder.onNext(byteBuf);
    } else {
      // Not yet handshaked. Are we handshaking now?
      handshaked = readHandshake(byteBuf);
      if (handshaked) {
        // Yes, we got handshaked so we need to send the settings frame accor
        // sendSettingsFrame();
      }
    }
  }

  private boolean readHandshake(ByteBuf byteBuf) {
    if (handshakeBuffer == null && byteBuf.readableBytes() >= CONNECTION_PREFACE_LENGTH) {
      return isPrefaceCorrect(byteBuf);
    }

    if (handshakeBuffer == null) {
      handshakeBuffer = Unpooled.buffer(CONNECTION_PREFACE_LENGTH, CONNECTION_PREFACE_LENGTH);
    }

    int missingBytes = CONNECTION_PREFACE_LENGTH - handshakeBuffer.readableBytes();
    if (byteBuf.readableBytes() >= missingBytes) {
      byteBuf.readBytes(handshakeBuffer, missingBytes);
      return isPrefaceCorrect(handshakeBuffer);
    } else {
      byteBuf.readBytes(handshakeBuffer);
      return false;
    }
  }

  private boolean isPrefaceCorrect(ByteBuf byteBuf) {
    return EXPECTED_PREFACE.compareTo(byteBuf) == 0;
  }

  @Override
  public void onError(Throwable e) {
    observer.onError(e);
  }

  @Override
  public void onComplete() {
    observer.onComplete();
  }

  public static Http2Processor createFor(Channel channel) {
    return new Http2Processor(channel, new FrameDecoderImpl());
  }
}
