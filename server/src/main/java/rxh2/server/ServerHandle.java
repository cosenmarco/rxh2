package rxh2.server;

import java.util.concurrent.ExecutionException;

/**
 * Instances of the ServerHandle let you control the server after creation. Instances must be thread
 * safe.
 */
public interface ServerHandle {

  /**
   * This method should tear down the activity on the server and block until the shutdownGracefully
   * is completed.
   */
  void shutdownGracefully();

  /**
   * Blocks until the server was shutdownGracefully and all resources freed up.
   */
  void join() throws ExecutionException, InterruptedException;
}
