package sh.okx.gottfried;

import com.github.steveice10.packetlib.Session;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException;
import java.util.Iterator;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import sh.okx.gottfried.action.Action;
import sh.okx.gottfried.action.CompletableAction;
import sh.okx.gottfried.action.WaitAction;

public class Heartbeat {
  private final Session session;
  private final Queue<CompletableAction> actions = new ConcurrentLinkedQueue<>();

  public Heartbeat(Session session) {
    this.session = session;
  }

  public Completable queue(Action action) {
    return Completable.create(emitter -> {
      CompletableAction caction = new CompletableAction(action, emitter);
      synchronized (this.actions) {
        this.actions.add(caction);
      }
      emitter.setDisposable(new Disposable() {
        @Override
        public void dispose() {
          synchronized (Heartbeat.this.actions) {
            Heartbeat.this.actions.remove(caction);
          }
        }

        @Override
        public boolean isDisposed() {
          synchronized (Heartbeat.this.actions) {
            return Heartbeat.this.actions.contains(caction);
          }
        }
      });
    });
  }

  public void clear() {
    synchronized (this.actions) {
      for (CompletableAction action : this.actions) {
        try {
          action.getEmitter().tryOnError(new IllegalStateException("Cleared"));
        } catch (OnErrorNotImplementedException e) {
          // this is fine
        }
      }
      this.actions.clear();
    }
  }

  public double f() {
    return 0;
  }

  public Completable waitTicks(int ticks) {
    return this.queue(new WaitAction(ticks));
  }

  public void run() {
    if (!session.isConnected()) {
      return;
    }

    synchronized (this.actions) {
      Iterator<CompletableAction> it = this.actions.iterator();
      while (it.hasNext()) {
        CompletableAction completableAction = it.next();
        Action action = completableAction.getAction();
        try {
          action.tick();
        } catch (Throwable t) {
          completableAction.getEmitter().onError(t);
          it.remove();
          continue;
        }
        if (action.isComplete()) {
          completableAction.getEmitter().onComplete();
          it.remove();
        }
      }
    }
  }
}
