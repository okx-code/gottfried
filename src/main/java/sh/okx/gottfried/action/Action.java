package sh.okx.gottfried.action;

public interface Action {
  void tick();
  boolean isComplete();
}
