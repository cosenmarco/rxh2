package rxh2.server.impl;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NonNull;

@Data
public class OutboundByteBuf {
  @NonNull
  private final ByteBuf buffer;
  private final boolean flushNow;
}
