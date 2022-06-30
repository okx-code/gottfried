package sh.okx.gottfried.coords;

public class Location extends Position {
  private final float yaw;
  private final float pitch;

  public static Location updatePosition(Position position, Location location) {
    return new Location(position.getX(), position.getY(), position.getZ(), location.getYaw(), location.getPitch());
  }

  public Location(double x, double y, double z, float yaw, float pitch) {
    super(x, y, z);
    this.yaw = yaw;
    this.pitch = pitch;
  }

  public float getYaw() {
    return this.yaw;
  }

  public float getPitch() {
    return this.pitch;
  }

  public Location setYaw(float yaw) {
    return new Location(this.getX(), this.getY(), this.getZ(), yaw, this.pitch);
  }

  public Location setPitch(float pitch) {
    return new Location(this.getX(), this.getY(), this.getZ(), this.yaw, pitch);
  }
}
