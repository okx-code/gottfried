package sh.okx.gottfried.action;

import sh.okx.gottfried.ServerConnection;
import sh.okx.gottfried.coords.Position;

public class PathAction implements Action {
  private final ServerConnection connection;
  private final double speed;
  private final Position[] path;

  private boolean complete = false;

  private MoveToAction action;
  private int index = 0;

  public PathAction(ServerConnection connection, double speed, Position... path) {
    this.connection = connection;
    this.speed = speed;
    this.path = path;
    setAction();
  }

  private void setAction() {
    this.action = new MoveToAction(this.connection, this.path[this.index], this.speed);
  }

  @Override
  public void tick() {
    if (this.action.isComplete()) {
      this.index++;
      if (this.index >= this.path.length) {
        this.complete = true;
        return;
      }

      setAction();
    }
    this.action.tick();
  }

  @Override
  public boolean isComplete() {
    return this.complete;
  }
}
