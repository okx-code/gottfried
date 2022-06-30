package sh.okx.gottfried;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class EventBus {
  private boolean stopped = false;

  public EventBus() {
  }

  private final Subject<Object> subject = PublishSubject.create().toSerialized();

  public <T> Observable<T> observe(Class<T> clazz) {
    return this.subject//.subscribeOn()
        .filter(clazz::isInstance)
        .map(clazz::cast);
  }

  public <T> void push(T event) {
    if (!this.stopped) {
      this.subject.onNext(event);
    }
  }

  public void stop() {
    this.stopped = true;
  }
}
