package rxh2.server.impl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.concurrent.ExecutionException;
import lombok.Data;
import rxh2.server.ServerHandle;
import rxh2.server.h2.Http2Processor;

public class ServerImpl implements ServerHandle {

  private static final InternalLogger logger = InternalLoggerFactory
      .getInstance(ServerImpl.class);

  private final ServerConfig config;
  private volatile EventLoopGroup bossGroup;
  private volatile EventLoopGroup workerGroup;

  public ServerImpl(ServerConfig config) {
    this.config = config;
  }

  public ServerImpl bind() {
    bossGroup = new NioEventLoopGroup(1,
        new DefaultThreadFactory("rxh2-boss"));

    workerGroup = new NioEventLoopGroup(0,
        new DefaultThreadFactory("rxh2-worker"));

    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .option(ChannelOption.SO_BACKLOG, 100)
          .handler(new LoggingHandler(LogLevel.INFO))
          .childHandler(
              new MainChannelInitializer((Channel channel) -> new Http2Processor(channel)));

      // Start the server.
      try {
        b.bind(config.port).sync();
      } catch (InterruptedException e) {
        shutdownGracefully();
        Thread.currentThread().interrupt();
      }
    } catch (Exception e) {
      logger.error("Error while bootstrapping the server", e);
      shutdownGracefully();
    }

    return this;
  }

  @Override
  public void shutdownGracefully() {
    if (config.shutdownCallback != null) {
      config.shutdownCallback.run();
    }
    bossGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();
  }

  @Override
  public void join() throws ExecutionException, InterruptedException {
    bossGroup.terminationFuture().get();
    workerGroup.terminationFuture().get();
  }

  /**
   * Immutable server configuration data to instantiate a running server.
   */
  @Data
  public static class ServerConfig {

    private final int port;
    private final ChannelHandler serverChannelHandler;
    private final Runnable shutdownCallback;
//    private final Subject<ChannelEvent> channelProcessor;
  }
}
