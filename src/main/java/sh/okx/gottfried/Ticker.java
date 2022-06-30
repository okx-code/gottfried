package sh.okx.gottfried;

import java.util.TimerTask;

public class Ticker extends TimerTask {
  private final ServerConnection connection;

  public Ticker(ServerConnection connection) {
    this.connection = connection;
  }

  @Override
  public void run() {
    connection.getHeartbeat().run();
    // todo handle movement
  }
}
