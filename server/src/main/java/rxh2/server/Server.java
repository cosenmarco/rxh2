package rxh2.server;

import io.netty.channel.ChannelHandler;
import rxh2.server.impl.ServerImpl;

/**
 * Builder for the server. This class is not thread safe.
 */
public class Server {

  private int port = 0;
  private Runnable shutdownCallback;
  private ChannelHandler serverChannelHandler;
  private boolean secure;

  private Server() {
  }

  /**
   * Factory method for creating a new Server builder
   */
  public static Server builder() {
    return new Server();
  }

  /**
   * Assigns a port to the server we want to build.
   *
   * @param port an integer used to bind the server. If 0 will bind to a random port > 1024.
   */
  public Server port(int port) {
    this.port = port;
    return this;
  }

  /**
   * Installs a ChannelHandler to the underlying Netty server. Maybe in the future introduce backend
   * independent event handlers.
   *
   * @param serverChannelHandler the ChannelHandler to set for the NioServerSocketChannel
   */
  public Server serverChannelHandler(ChannelHandler serverChannelHandler) {
    this.serverChannelHandler = serverChannelHandler;
    return this;
  }

  public Server secure() {
    this.secure = true;
    return this;
  }

  /**
   * Binds to the specified port and starts the event processing.
   *
   * @return a ServerHandle to control the server
   */
  public ServerHandle bind() {
    return new ServerImpl(
        new ServerImpl.ServerConfig(
            port,
            serverChannelHandler,
            shutdownCallback,
            secure)
    ).bind();
  }

  /**
   * A callback which is called when the shutdown is requested on the ServerHandler. Please note
   * that the callback could run in a different thread than the thread which owns the ServerHandle
   * and sends the shutdownGracefully() signal
   *
   * @param callback the callback to use
   */
  public Server shutdownCallback(Runnable callback) {
    shutdownCallback = callback;
    return this;
  }
}
