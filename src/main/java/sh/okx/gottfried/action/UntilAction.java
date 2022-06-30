package sh.okx.gottfried.action;

import java.util.function.BooleanSupplier;

public class UntilAction implements Action {
  private final BooleanSupplier checker;
  private boolean complete = false;

  public UntilAction(BooleanSupplier checker) {
    this.checker = checker;
  }

  @Override
  public void tick() {
    if (!this.complete) {
      this.complete = this.checker.getAsBoolean();
    }
  }

  @Override
  public boolean isComplete() {
    return this.complete;
  }
}
