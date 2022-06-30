package sh.okx.gottfried.listener;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPingPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundPongPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import sh.okx.gottfried.ServerConnection;
import sh.okx.gottfried.coords.Location;
import sh.okx.gottfried.events.BreakTooFastEvent;
import sh.okx.gottfried.events.ConnectedEvent;
import sh.okx.gottfried.farm.OakFarm;
import sh.okx.gottfried.player.Inventory;
import sh.okx.gottfried.player.Player;
import sh.okx.gottfried.player.PlayerInventory;

public class LoginListener extends SessionAdapter {

  private final ServerConnection connection;
  private boolean firstPosition = true;

  public LoginListener(ServerConnection connection) {
    this.connection = connection;
  }

  @Override
  public void packetReceived(Session session, Packet packet) {
    Player player = this.connection.getPlayer();
    if (packet instanceof ClientboundLoginPacket loginPacket) {
      this.connection.connected();

      player.setEntityId(loginPacket.getEntityId());

      this.connection.getBus().push(new ConnectedEvent());

    } else if (packet instanceof ClientboundPlayerPositionPacket posRotPacket) {
      double y = posRotPacket.getY();
      Location loc = new Location(posRotPacket.getX(), y, posRotPacket.getZ(), posRotPacket.getYaw(), posRotPacket.getPitch());
      player.setLocation(loc);
      this.connection.sendPacket(new ServerboundAcceptTeleportationPacket(posRotPacket.getTeleportId()));
      System.out.println("Moved to: " + loc);

      if (this.firstPosition) {
        this.firstPosition = false;
        // start farms here
        // TODO wait for you have been combat tagged
        new Timer().schedule(new TimerTask() {
          @Override
          public void run() {
            System.out.println("Starting oak farm");
            new OakFarm(connection).start();
          }
        }, 3000);
      }
    } else if (packet instanceof ClientboundContainerSetContentPacket windowPacket) {
      if (player.getOpenInventory() != null) {
        Inventory open = player.getOpenInventory();
        if (open.getId() == windowPacket.getContainerId()) {

          ItemStack[] packetItems = windowPacket.getItems();
          open.setItems(Arrays.copyOfRange(packetItems, 0, packetItems.length - 36));
          ItemStack[] pitems = player.getInventory().getItems();
          System.arraycopy(Arrays.copyOfRange(packetItems, packetItems.length - 36, packetItems.length), 0, pitems, 9, 36);
          player.getInventory().setItems(pitems);
        }
      } else if (windowPacket.getContainerId() == 0) {
        player.getInventory().setItems(windowPacket.getItems());
      }
    } else if (packet instanceof ClientboundOpenScreenPacket screenPacket) {
      player.setOpenInventory(new Inventory(screenPacket.getContainerId(), screenPacket.getType(), screenPacket.getName()));
    } else if (packet instanceof ClientboundSetHealthPacket infoPacket) {
      float health = infoPacket.getHealth();
      System.out.println("Health updated to " + health);
      if (health < player.getHealth() && health < 18) {
        this.connection.flagReconnect();
        this.connection.disconnected("Low health");
        return;
      }

      player.setHealth(infoPacket.getHealth());
      player.setFood(infoPacket.getFood());
    } else if (packet instanceof ClientboundContainerSetSlotPacket slotPacket) {
      if (player.getOpenInventory() != null) {
        Inventory open = player.getOpenInventory();
        if (open.getId() == slotPacket.getContainerId() && open.isInitialized()) {
          boolean isPlayer = slotPacket.getSlot() >= open.getItems().length;
          ItemStack[] items = (isPlayer ? player.getInventory().getItems() : open.getItems());
          items[slotPacket.getSlot() - (isPlayer ? (open.getItems().length - 9) : 0)] = slotPacket.getItem();
          if (isPlayer) {
            player.getInventory().setItems(items);
          } else {
            open.setItems(items);
          }
          return;
        }
      }

      if (slotPacket.getContainerId() != 0 && slotPacket.getContainerId() != -2) return;

      PlayerInventory inventory = player.getInventory();
      ItemStack[] items = inventory.getItems();
      items[slotPacket.getSlot()] = slotPacket.getItem();
      inventory.setItems(items);
    } else if (packet instanceof ClientboundChatPacket chatPacket) {
      Component message = chatPacket.getMessage();
      String plain = PlainTextComponentSerializer.plainText().serialize(message);
      if (plain.equals("You are breaking blocks too fast")) {
        this.connection.getBus().push(new BreakTooFastEvent());
      }
    } else if (packet instanceof ClientboundPingPacket pingPacket) {
      connection.sendPacket(new ServerboundPongPacket(pingPacket.getId()));
    }
  }
}
