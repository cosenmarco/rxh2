package rxh2.server.h2.frame;

import io.netty.buffer.ByteBuf;
import lombok.Data;

@Data
public class Frame {
  private final FrameHeader header;
  private final ByteBuf payload;
}
