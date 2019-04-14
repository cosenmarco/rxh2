package rxh2.server.h2;

import io.netty.buffer.ByteBuf;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import rxh2.server.h2.frame.Frame;

public abstract class FrameDecoder extends Observable<Frame> implements Observer<ByteBuf> {

}
