package rxh2.examples.server.echo;

import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rxh2.server.Server;
import rxh2.server.ServerHandle;

/**
 * A simple HTTP/2 Echo server which replays everything is sent over in the request body. Note that
 * this implements the correct way of shutting down the server in case of a OS signal (like when
 * pressing ctrl+c in the console)
 */
public class EchoServer {

  private static Logger log = LoggerFactory.getLogger(EchoServer.class);

  public static void main(String[] args) {
    Thread mainThread = Thread.currentThread();

    final ServerHandle serverHandle = Server.builder()
        .port(8888)
        .serverChannelHandler(new LoggingHandler(LogLevel.INFO))
        .shutdownCallback(() -> log.info("Shutting down server!"))
        .secure()
        .bind();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      serverHandle.shutdownGracefully();
      log.info("Called shutdownGracefully() in shutdown hook. Waiting for main thread to finish.");
      try {
        mainThread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        LogManager.shutdown();
      }
    }, "server-shutdown-hook"));

    log.info("Server started!");

    try {
      serverHandle.join();
      log.info("Server stopped!");
    } catch (ExecutionException | InterruptedException e) {
      log.info("Exception caught in server join()", e);
      mainThread.interrupt();
    }
  }
}
