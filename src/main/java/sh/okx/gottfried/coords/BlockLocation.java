package sh.okx.gottfried.coords;

import java.util.Objects;

public class BlockLocation {

  private final int x;
  private final int y;
  private final int z;

  public BlockLocation(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public int getX() {
    return this.x;
  }

  public int getY() {
    return this.y;
  }

  public int getZ() {
    return this.z;
  }

  public BlockLocation setX(int x) {
    return new BlockLocation(x, this.y, this.z);
  }

  public BlockLocation setY(int y) {
    return new BlockLocation(this.x, y, this.z);
  }

  public BlockLocation setZ(int z) {
    return new BlockLocation(this.x, this.y, z);
  }

  public BlockLocation add(int x, int y, int z) {
    return new BlockLocation(this.x + x, this.y + y, this.z + z);
  }

  public BlockLocation add(BlockLocation location) {
    return new BlockLocation(this.x + location.x, this.y + location.y, this.z + location.z);
  }

  public BlockLocation subtract(int x, int y, int z) {
    return new BlockLocation(this.x - x, this.y - y, this.z - z);
  }

  public BlockLocation subtract(BlockLocation location) {
    return new BlockLocation(this.x - location.x, this.y - location.y, this.z - location.z);
  }

  public BlockLocation multiply(int m) {
    return new BlockLocation(this.x * m, this.y * m, this.z * m);
  }

  public BlockLocation multiply(BlockLocation location) {
    return new BlockLocation(location.x * this.x, location.y * this.y, location.z * this.z);
  }

  public boolean isAABB(BlockLocation a, BlockLocation b) {
    return Math.min(a.getX(), b.getX()) <= this.x
        && Math.max(a.getX(), b.getX()) >= this.x
        && Math.min(a.getY(), b.getY()) <= this.y
        && Math.max(a.getY(), b.getY()) >= this.y
        && Math.min(a.getZ(), b.getZ()) <= this.z
        && Math.max(a.getZ(), b.getZ()) >= this.z;
  }

  /**
   * Checks if the signum is the same for all valids
   */
  public boolean isSignum(BlockLocation location) {
    return Integer.signum(this.x) == Integer.signum(location.x)
        && Integer.signum(this.y) == Integer.signum(location.y)
        && Integer.signum(this.z) == Integer.signum(location.z);
  }

  public int select(BlockLocation selector) {
    if (selector.x != 0 && selector.y != 0
        || selector.x != 0 && selector.z != 0
        || selector.y != 0 && selector.z != 0) {
      throw new IllegalArgumentException("Not valid selection: " + selector);
    }

    int x = this.x * selector.x;
    int y = this.y * selector.y;
    int z = this.z * selector.z;

    return x + y + z;
  }

  public Position centre() {
    return new Position(this.x + 0.5, this.y, this.z + 0.5);
  }

  public Position toPosition() {
    return new Position(this.x, this.y, this.z);
  }

  public com.github.steveice10.mc.protocol.data.game.entity.metadata.Position toProtocolPosition() {
    return new com.github.steveice10.mc.protocol.data.game.entity.metadata.Position(this.x, this.y, this.z);
  }

  @Override
  public String toString() {
    return "BlockLocation{" +
        "x=" + this.x +
        ", y=" + this.y +
        ", z=" + this.z +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BlockLocation that = (BlockLocation) o;
    return this.x == that.x && this.y == that.y && this.z == that.z;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.x, this.y, this.z);
  }
}
