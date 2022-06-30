package sh.okx.gottfried.player;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerType;

public class Inventory {
  private final int id;
  private final ContainerType type;
  private final String name;
  private boolean initialized = false;
  private int counter = 1;
  private ItemStack[] items;

  public Inventory(int id, ContainerType type, String name) {
    this.id = id;
    this.type = type;
    this.name = name;
  }

  public int getId() {
    return this.id;
  }

  public ContainerType getType() {
    return this.type;
  }

  public String getName() {
    return this.name;
  }

  public boolean isInitialized() {
    return this.initialized;
  }

  public ItemStack[] getItems() {
    return this.items;
  }

  public void setItems(ItemStack[] items) {
    this.items = items;
    this.initialized = true;
  }

  public int getCounter() {
    return this.counter;
  }

  public void setCounter(int counter) {
    this.counter = counter;
  }
}
