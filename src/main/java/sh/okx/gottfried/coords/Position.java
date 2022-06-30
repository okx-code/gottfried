package sh.okx.gottfried.coords;

public class Position {
  public static Position ONE = new Position(1, 1, 1);

  private final double x;
  private final double y;
  private final double z;

  public Position(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public double getX() {
    return this.x;
  }

  public double getY() {
    return this.y;
  }

  public double getZ() {
    return this.z;
  }

  public Position multiply(double m) {
    return new Position(this.x * m, this.y * m, this.z * m);
  }

  public Position multiply(double x, double y, double z) {
    return new Position(x * this.x, y * this.y, z * this.z);
  }

  public Position add(double x, double y, double z) {
    return new Position(x + this.x, y + this.y, z + this.z);
  }

  public Position subtract(double x, double y, double z) {
    return new Position(this.x - x, this.y - y, this.z - z);
  }

  public Position add(Position position) {
    return this.add(position.x, position.y, position.z);
  }

  public Position subtract(Position position) {
    return subtract(position.x, position.y, position.z);
  }

  public Position multiply(Position position) {
    return multiply(position.x, position.y, position.z);
  }

  public BlockLocation toBlockLocation() {
    return new BlockLocation((int) Math.floor(this.x), (int) Math.floor(this.y), (int) Math.floor(this.z));
  }

  @Override
  public String toString() {
    return "Position{" +
        "x=" + this.x +
        ", y=" + this.y +
        ", z=" + this.z +
        '}';
  }

  public Position setY(double y) {
    return new Position(this.x, y, this.z);
  }
}
