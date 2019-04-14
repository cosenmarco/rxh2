package rxh2.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * This class is a Netty channel initializer which forwards the channel inbound events to
 * the channelProcessor and subscribes to the channelProcessor consuming ByteBufs
 * which are sent back in to the channel.
 */
class MainChannelInitializer extends ChannelInitializer<SocketChannel> {

  private static final InternalLogger logger = InternalLoggerFactory
      .getInstance(MainChannelInitializer.class);

  private static final AttributeKey<Object> PUBLISHER_KEY = AttributeKey.valueOf("publisher");

  private final ChannelProcessorFactory channelProcessorFactory;

  MainChannelInitializer(ChannelProcessorFactory channelProcessorFactory) {
    this.channelProcessorFactory = channelProcessorFactory;
  }

  @Override
  protected void initChannel(SocketChannel ch) {

    ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRegistered(ChannelHandlerContext ctx) {
        final Subject<ByteBuf> publisher = PublishSubject.create();
        final ChannelProcessor processor = channelProcessorFactory.buildProcessor(ctx.channel());

        // The publisher belongs to the channel
        ctx.channel().attr(PUBLISHER_KEY).set(publisher);
        publisher.subscribe(processor);

        processor.subscribe(new Observer<ByteBuf>() {
          @Override
          public void onNext(ByteBuf channelEvent) {
            logger.debug("invoked onNext()");
            ctx.writeAndFlush(channelEvent);
          }

          @Override
          public void onSubscribe(Disposable s) {
          }

          @Override
          public void onError(Throwable t) {
            logger.error("Error in Channel Observer", t);
            ctx.channel().close();
          }

          @Override
          public void onComplete() {
            logger.debug("invoked onComplete()");
            ctx.channel().close();
          }
        });
      }

      @Override
      public void channelUnregistered(ChannelHandlerContext ctx) {
        logger.debug("channelUnregistered()");
        Observer publisher = (Observer) ctx.channel().attr(PUBLISHER_KEY)
            .getAndSet(null);
        publisher.onComplete();
      }

      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) {
        logger.debug("channelRead()");
        Observer<ByteBuf> publisher = (Observer<ByteBuf>) ctx.channel()
            .attr(PUBLISHER_KEY)
            .get();
        publisher.onNext((ByteBuf) msg);
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.debug("exceptionCaught()");
        Observer publisher = (Observer) ctx.channel().attr(PUBLISHER_KEY)
            .get();
        publisher.onError(cause);
      }
    });
  }
}
