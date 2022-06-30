package sh.okx.gottfried.listener;

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import sh.okx.gottfried.ServerConnection;
import sh.okx.gottfried.events.FinishUseItemEvent;
import sh.okx.gottfried.player.Player;

public class EventListener extends SessionAdapter {

  private final ServerConnection connection;

  public EventListener(ServerConnection connection) {
    this.connection = connection;
  }

  @Override
  public void packetReceived(Session session, Packet packet) {
    Player player = connection.getPlayer();
    if (packet instanceof ClientboundEntityEventPacket clientboundEntityEventPacket) {
      if (clientboundEntityEventPacket.getEntityId() != player.getEntityId()) return;

      if (clientboundEntityEventPacket.getStatus() == EntityEvent.PLAYER_FINISH_USING_ITEM) {
        this.connection.getBus().push(new FinishUseItemEvent());
      }
    }
  }
}
