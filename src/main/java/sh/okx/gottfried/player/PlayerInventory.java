package sh.okx.gottfried.player;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.github.steveice10.mc.protocol.data.game.inventory.DropItemAction;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import java.util.Collections;
import sh.okx.gottfried.ServerConnection;

public class PlayerInventory {
  private final ServerConnection connection;
  private ItemStack[] items = new ItemStack[46];
  private boolean initialized = false;
  private int counter = 1;

  public PlayerInventory(ServerConnection connection) {
    this.connection = connection;
  }

  public ItemStack[] getItems() {
    return this.items;
  }

  public void setItems(ItemStack[] items) {
    this.items = items;
    this.initialized = true;
  }

  public void drop(int slot) {
    this.connection.sendPacket(new ServerboundContainerClickPacket(0, this.counter++, slot, ContainerActionType.DROP_ITEM,
        DropItemAction.DROP_SELECTED_STACK, null, Collections.emptyMap()));
    this.items[slot] = null;
  }

  public boolean isInitialized() {
    return this.initialized;
  }

  public int getCounter() {
    return this.counter;
  }

  public void setCounter(int counter) {
    this.counter = counter;
  }
}
