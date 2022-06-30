package sh.okx.gottfried.action;

public class WaitAction implements Action {
  private final int delay;
  private int ticks = 0;

  public WaitAction(int delay) {
    this.delay = delay;
  }

  @Override
  public void tick() {
    ++this.ticks;
  }

  @Override
  public boolean isComplete() {
    return this.ticks >= this.delay;
  }
}
