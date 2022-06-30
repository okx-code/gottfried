package sh.okx.gottfried.player;

import sh.okx.gottfried.ServerConnection;
import sh.okx.gottfried.coords.Location;

public class Player {
  private final ServerConnection connection;
  private final PlayerInventory inventory;
  private Inventory openInventory;
  private int entityId;

  private float health;

  private int food;
  private Location location;

  private boolean onGround;

  public Player(ServerConnection connection) {
    this.connection = connection;
    this.inventory = new PlayerInventory(connection);
  }

  public void tick() {

  }

  public Location getLocation() {
    return this.location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public PlayerInventory getInventory() {
    return this.inventory;
  }

  public void setHealth(float health) {
    this.health = health;
  }

  public float getHealth() {
    return this.health;
  }

  public int getFood() {
    return this.food;
  }

  public void setFood(int food) {
    this.food = food;
  }

  public int getEntityId() {
    return this.entityId;
  }

  public void setEntityId(int entityId) {
    this.entityId = entityId;
  }

  public void setOpenInventory(Inventory openInventory) {
    this.openInventory = openInventory;
  }

  public Inventory getOpenInventory() {
    return this.openInventory;
  }

  public boolean isOnGround() {
    return onGround;
  }

  public void setOnGround(boolean onGround) {
    this.onGround = onGround;
  }
}
