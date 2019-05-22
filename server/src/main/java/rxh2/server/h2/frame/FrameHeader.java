package rxh2.server.h2.frame;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a HTTPs frame header and it contains utility methods to decode
 * and encode it
 */
@Data
public class FrameHeader {
  private final int length;
  private final Type type;
  private final byte flags;
  private final int streamId;

  /**
   * Builds a {@link FrameHeader} from its components. For practical reasons length and type
   * are passed together in an int.
   * @param length an integer containing length
   * @param flags the flags byte
   * @param streamId the stream ID (and the reserved bit)
   * @return a new non-null {@link FrameHeader} representing the fields
   */
  public static FrameHeader fromBytes(int length, byte type, byte flags, int streamId) {
    return new FrameHeader(
        length & 0x0FFF,
        Type.from(type),
        flags,
        streamId & 0x7FFF);
  }

  @AllArgsConstructor
  public enum Type {
    PREFACE((byte)0xFF),
    DATA((byte)0x0),
    HEADERS((byte)0x01),
    PRIORITY((byte)0x02),
    RST_STREAM((byte)0x03),
    SETTINGS((byte)0x04),
    PUSH_PROMISE((byte)0x05),
    PING((byte)0x06),
    GOAWAY((byte)0x07),
    WINDOW_UPDATE((byte)0x08),
    CONTINUATION((byte)0x09);

    private final byte type;

    private static Type[] types = new Type[]{ DATA, HEADERS, PRIORITY, RST_STREAM, SETTINGS,
        PUSH_PROMISE, PING, GOAWAY, WINDOW_UPDATE, CONTINUATION};

    static Type from(byte typeId) {
      if (typeId < types.length && typeId >= 0) {
        return types[typeId];
      }
      throw new IllegalArgumentException("Unrecognized HTTP2 frame type: " + typeId);
    }
  }

  public enum Flags {
    END_STREAM((byte)0x01),
    ACK((byte)0x01),
    END_HEADERS((byte)0x04),
    PADDED((byte)0x08),
    PRIORITY((byte)0x20);

    private byte mask;

    Flags(byte mask) {
      this.mask = mask;
    }

    public boolean isSetInByte(byte flags) {
      return (flags & mask) > 0;
    }
  }
}
