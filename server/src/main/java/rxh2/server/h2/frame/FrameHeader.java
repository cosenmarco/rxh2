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
  private final Flags flags;
  private final int streamId;

  /**
   * Builds a {@link FrameHeader} from its components. For practical reasons length and type
   * are passed together in an int.
   * @param lengthAndType a 32bit integer containing length and type
   * @param flags the flags byte
   * @param streamId the stream ID (and the reserved bit)
   * @return a new non-null {@link FrameHeader} representing the fields
   */
  public static FrameHeader fromBytes(int lengthAndType, byte flags, int streamId) {
    return new FrameHeader(
        lengthAndType & 0x0FFF,
        Type.from((byte)((lengthAndType & 0xF000) >> 24)),
        new Flags(flags),
        streamId & 0x7FFF);
  }

  @AllArgsConstructor
  public enum Type {
    DATA((byte)0x0),
    HEADERS((byte)0x01);

    private final byte type;

    private static Type[] types = new Type[]{ DATA, HEADERS };

    static Type from(byte typeId) {
      if (typeId < types.length) {
        return types[typeId];
      }
      throw new IllegalArgumentException("Unrecognized HTTP2 frame type: " + typeId);
    }
  }

  @Data
  public static class Flags {
    private final byte flags;
  }
}
