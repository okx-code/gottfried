package sh.okx.gottfried;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import java.util.Timer;
import java.util.TimerTask;
import sh.okx.gottfried.player.Player;

public class ServerConnection {
  private final EventBus bus = new EventBus();
  private final Session session;
  private final Player player;

  private Ticker ticker;
  private Heartbeat heartbeat;
  private boolean connected = false;

  public ServerConnection(Session session) {
    this.session = session;
    this.player = new Player(this);
  }

  public void connected() {
    this.connected = true;
    this.heartbeat = new Heartbeat(this.session);
    new Timer("Ticker").schedule(this.ticker = new Ticker(this), 50, 50);
  }

  public void disconnected(String s) {
    this.connected = false;
    this.session.disconnect(s);
    this.heartbeat.clear();
    this.ticker.cancel();
    this.bus.stop();
  }

  public void flagReconnect() {
    this.session.setFlag("reconnect", true);
  }

  public void sendPacket(Packet packet) {
    this.session.send(packet);
  }

  public Player getPlayer() {
    return this.player;
  }

  public Session getSession() {
    return session;
  }

  public EventBus getBus() {
    return this.bus;
  }

  public Heartbeat getHeartbeat() {
    return heartbeat;
  }
}
