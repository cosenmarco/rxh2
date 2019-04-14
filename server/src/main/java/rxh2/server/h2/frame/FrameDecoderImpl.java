package rxh2.server.h2.frame;

import io.netty.buffer.ByteBuf;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import rxh2.server.h2.FrameDecoder;

/**
 * This class
 */
public class FrameDecoderImpl extends FrameDecoder {

  @Override
  public void onSubscribe(Disposable d) {

  }

  @Override
  public void onNext(ByteBuf byteBuf) {

  }

  @Override
  public void onError(Throwable e) {

  }

  @Override
  public void onComplete() {

  }

  @Override
  protected void subscribeActual(Observer<? super Frame> observer) {

  }
}
