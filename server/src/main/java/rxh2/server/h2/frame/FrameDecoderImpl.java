package rxh2.server.h2.frame;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import rxh2.server.h2.frame.FrameHeader.Type;

/**
 * This class decodes a stream of {@link ByteBuf}s into a stream of Http2 Frames
 * handling the special case of the connection preface.
 *
 * It accumulates bytes by copying them into internal buffers which are then used to decode
 * the frames.
 *
 * Note that this Processor is made to have just one subscriber at any given point in time
 * which must be subscribed before start receiving buffers.
 */
public class FrameDecoderImpl implements Processor<ByteBuf, Frame> {

  private final static int FRAME_HEADER_SIZE = 9;
  private final static int CONNECTION_PREFACE_LENGTH = 24;

  // A fake frame header for the preface frame
  private final static FrameHeader PREFACE_HEADER = new FrameHeader(
      CONNECTION_PREFACE_LENGTH, Type.PREFACE, (byte)0, 0);

  private final static ByteBuf EXPECTED_PREFACE = Unpooled.wrappedBuffer(new byte[]{
      0x50, 0x52, 0x49, 0x20, 0x2a, 0x20, 0x48, 0x54, 0x54, 0x50, 0x2f, 0x32, 0x2e,
      0x30, 0x0d, 0x0a, 0x0d, 0x0a, 0x53, 0x4d, 0x0d, 0x0a, 0x0d, 0x0a
  });

  private boolean decodingHeader = false;
  private int missingBytes = CONNECTION_PREFACE_LENGTH;
  private FrameHeader currentHeader = PREFACE_HEADER;
  private ByteBuf frameHeaderBuffer = null;
  private ByteBuf framePayloadBuffer = Unpooled
      .buffer(CONNECTION_PREFACE_LENGTH, CONNECTION_PREFACE_LENGTH);
  private Subscriber<? super Frame> subscriber;

  @Override
  public void onSubscribe(Subscription s) {
    // Nothing to do as upstream doesn't handle back pressure
  }

  public void onNext(ByteBuf byteBuf) {
    if (byteBuf == null || byteBuf.readableBytes() <= 0) {
      return;
    }

    while (byteBuf.readableBytes() > 0) {
      if (byteBuf.readableBytes() >= missingBytes) {
        readSomeBytes(byteBuf, missingBytes);

        // Flip-flop between decoding header and payload
        if (decodingHeader) {
          buildHeader();
          createNewPayloadBuffer();
          missingBytes = currentHeader.getLength();
          if (missingBytes == 0) {
            // Empty frame
            frameCompleted();
          } else {
            decodingHeader = false;
          }
        } else {
          frameCompleted();
        }
      } else {
        final int bytesToRead = byteBuf.readableBytes();
        readSomeBytes(byteBuf, bytesToRead);
        missingBytes -= bytesToRead;
      }
    }
  }

  private void frameCompleted() {
    emitFrame();
    creteNewHeaderBuffer();
    missingBytes = FRAME_HEADER_SIZE;
    decodingHeader = true;
  }

  private void creteNewHeaderBuffer() {
    frameHeaderBuffer = Unpooled
        .buffer(FRAME_HEADER_SIZE, FRAME_HEADER_SIZE);
  }

  private void createNewPayloadBuffer() {
    framePayloadBuffer = Unpooled
        .buffer(currentHeader.getLength(), currentHeader.getLength());
  }

  private void buildHeader() {
    currentHeader = FrameHeader.fromBytes(
        frameHeaderBuffer.readByte() |
            frameHeaderBuffer.readByte() >> 8 |
            frameHeaderBuffer.readByte() >> 16,
        frameHeaderBuffer.readByte(),
        frameHeaderBuffer.readByte(),
        frameHeaderBuffer.readInt()
    );
  }

  private void emitFrame() {
    subscriber.onNext(new Frame(currentHeader, framePayloadBuffer));
  }

  private void readSomeBytes(ByteBuf byteBuf, int bytesToRead) {
    if (decodingHeader) {
      byteBuf.readBytes(frameHeaderBuffer, bytesToRead);
    } else {
      byteBuf.readBytes(framePayloadBuffer, bytesToRead);
    }
  }

  @Override
  public void onError(Throwable t) {
    // Let the subscriber decide what to do with the error
    subscriber.onError(t);
  }

  @Override
  public void onComplete() {
    subscriber.onComplete();
  }

  @Override
  public void subscribe(Subscriber<? super Frame> s) {
    subscriber = s;
  }
}
