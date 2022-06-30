package sh.okx.gottfried.action;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import sh.okx.gottfried.ServerConnection;
import sh.okx.gottfried.coords.Location;
import sh.okx.gottfried.coords.Position;

public class MoveToAction implements Action {

  private final static double errorMargin = 0.002D;
  public static final double SLOW_SPEED = 1.3D;
  public static final double WALKING_SPEED = 4.316D;
  public static final double SPRINTING_SPEED = 5.612D;
  public static final double FALLING = 20.0;

  private final ServerConnection connection;
  private final Position destination;
  private double movementSpeed;

  public static MoveToAction relativeBlock(ServerConnection connection, int relativeX, int relativeZ, double movementSpeed) {
    Position position = connection.getPlayer().getLocation();
    int blockX = (int) Math.floor(position.getX());
    int blockZ = (int) Math.floor(position.getZ());
    return new MoveToAction(connection, new Position(blockX + relativeX + 0.5, position.getY(), blockZ + relativeZ + 0.5), movementSpeed);
  }

  public MoveToAction(ServerConnection connection, Position destination) {
    this(connection, destination, WALKING_SPEED);
  }

  public MoveToAction(ServerConnection connection, Position destination, double movementSpeed) {
    this.connection = connection;
    this.destination = destination;
    this.movementSpeed = movementSpeed;
  }

  private Position getNextLocation(Position current) {
    double xDiff = this.destination.getX() - current.getX();
    // double yDiff = destination.getY() - current.getY();
    double zDiff = this.destination.getZ() - current.getZ();
    // connection.getPlayerStatus().setMidAir(yDiff > errorMargin);
    double distance = Math.sqrt((xDiff * xDiff) + (zDiff * zDiff));
    double timeTakenSeconds = distance / this.movementSpeed;
    double timeTakenTicks = timeTakenSeconds * 20;
    if (timeTakenTicks < 1.0) {
      timeTakenTicks = 1.0;
    }
    double deltaX = (xDiff / timeTakenTicks);
    double deltaZ = (zDiff / timeTakenTicks);
    return new Position(current.getX() + deltaX, this.destination.getY(), current.getZ() + deltaZ);
  }

  public boolean hasReachedDestination(Position current) {
    return Math.abs(current.getX() - this.destination.getX()) < errorMargin
        && Math.abs(current.getZ() - this.destination.getZ()) < errorMargin;
  }

  @Override
  public void tick() {
    Location loc = this.connection.getPlayer().getLocation();
    Position next = this.getNextLocation(loc);
    double y = next.getY();
    this.connection.sendPacket(new ServerboundMovePlayerPosRotPacket(true, next.getX(), y, next.getZ(), loc.getYaw(), loc.getPitch()));
    this.connection.getPlayer().setLocation(Location.updatePosition(next, loc));
  }

  @Override
  public boolean isComplete() {
    return this.hasReachedDestination(this.connection.getPlayer().getLocation());
  }

  @Override
  public String toString() {
    return "MoveToAction{" +
        "destination=" + destination +
        ", movementSpeed=" + movementSpeed +
        '}';
  }

  public void setSpeed(double i) {
    this.movementSpeed = i;
  }
}
