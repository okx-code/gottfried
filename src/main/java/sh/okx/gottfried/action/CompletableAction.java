package sh.okx.gottfried.action;

import io.reactivex.rxjava3.core.CompletableEmitter;

public class CompletableAction {
  private final Action action;
  private final CompletableEmitter emitter;

  public CompletableAction(Action action, CompletableEmitter emitter) {
    this.action = action;
    this.emitter = emitter;
  }

  public Action getAction() {
    return this.action;
  }

  public CompletableEmitter getEmitter() {
    return this.emitter;
  }
}
