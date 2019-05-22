package rxh2.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLException;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * This class is a Netty channel initializer which forwards the channel inbound events to
 * the channelProcessor and subscribes to the channelProcessor consuming ByteBufs
 * which are sent back in to the channel.
 */
class MainChannelInitializer extends ChannelInitializer<SocketChannel> {

  private static final InternalLogger logger = InternalLoggerFactory
      .getInstance(MainChannelInitializer.class);

  private static final AttributeKey<Object> PROCESSOR_KEY = AttributeKey.valueOf("processor");

  private final ChannelProcessorFactory channelProcessorFactory;
  private boolean secure;

  MainChannelInitializer(ChannelProcessorFactory channelProcessorFactory, boolean secure) {
    this.channelProcessorFactory = channelProcessorFactory;
    this.secure = secure;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws CertificateException, SSLException {

    if (secure) {
//      SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
      SelfSignedCertificate ssc = new SelfSignedCertificate();
      SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
          /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
           * Please refer to the HTTP/2 specification for cipher requirements. */
          .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
          .applicationProtocolConfig(new ApplicationProtocolConfig(
              Protocol.ALPN,
              // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
              SelectorFailureBehavior.NO_ADVERTISE,
              // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
              SelectedListenerFailureBehavior.ACCEPT,
              ApplicationProtocolNames.HTTP_2))
          .build();
      ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
    }

    ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRegistered(ChannelHandlerContext ctx) {
        final ChannelProcessor processor = channelProcessorFactory.buildProcessor(ctx.channel());

        // The processor belongs to the channel
        ctx.channel().attr(PROCESSOR_KEY).set(processor);

        processor.subscribe(new Subscriber<OutboundByteBuf>() {
          private Subscription subscription;

          @Override
          public void onSubscribe(Subscription s) {
            subscription = s;
            subscription.request(1);
          }

          @Override
          public void onNext(OutboundByteBuf outboundByteBuf) {
            ChannelFuture promise;
            if(outboundByteBuf.isFlushNow()) {
              logger.debug("ctx.writeAndFlush");
              promise = ctx.writeAndFlush(outboundByteBuf.getBuffer());
            } else {
              logger.debug("ctx.write");
              promise = ctx.write(outboundByteBuf.getBuffer());
            }
            promise.addListener((future) -> subscription.request(1));
          }

          @Override
          public void onError(Throwable t) {
            logger.error("Unrecoverable error in subscriber of ChannelProcessor."
                + "Closing channel {}", ctx.channel().id(), t);
            ctx.channel().close();
          }

          @Override
          public void onComplete() {
            logger.debug("onComplete()");
            ctx.channel().close();
          }
        });
      }

      @Override
      public void channelUnregistered(ChannelHandlerContext ctx) {
        logger.debug("channelUnregistered()");
        ChannelProcessor processor = (ChannelProcessor) ctx.channel().attr(PROCESSOR_KEY)
            .getAndSet(null);
        processor.onComplete();
      }

      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) {
        logger.debug("channelRead()");
        ChannelProcessor processor = (ChannelProcessor) ctx.channel().attr(PROCESSOR_KEY).get();
        processor.onNext((ByteBuf) msg);
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.debug("exceptionCaught()");
        ChannelProcessor processor = (ChannelProcessor) ctx.channel().attr(PROCESSOR_KEY).get();
        processor.onError(cause);
      }
    });
  }
}
