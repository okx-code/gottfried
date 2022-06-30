package sh.okx.gottfried.farm;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.github.steveice10.mc.protocol.data.game.inventory.MoveToHotbarAction;
import com.github.steveice10.mc.protocol.data.game.inventory.ShiftClickItemAction;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.ShortTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import io.reactivex.rxjava3.core.Completable;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import sh.okx.gottfried.ServerConnection;
import sh.okx.gottfried.action.MoveToAction;
import sh.okx.gottfried.action.UntilAction;
import sh.okx.gottfried.coords.BlockLocation;
import sh.okx.gottfried.coords.Location;
import sh.okx.gottfried.events.BreakTooFastEvent;
import sh.okx.gottfried.events.FinishUseItemEvent;
import sh.okx.gottfried.player.Inventory;
import sh.okx.gottfried.player.Items;
import sh.okx.gottfried.player.Player;
import sh.okx.gottfried.player.PlayerInventory;

public class OakFarm {

  private static final BlockLocation FARM_START = new BlockLocation(1856, 100, 4184);
  private static final BlockLocation FARM_END = new BlockLocation(1932, 100, 4118);
  private static final BlockLocation ROW = new BlockLocation(1, 0, 0);
  private static final BlockLocation COLUMN = new BlockLocation(0, 0, -1);

  private static final int SAPLING_ITEM = Items.OAK_SAPLING;
  private static final int AXE_ITEM = Items.DIAMOND_AXE;
  public static final int FIRST_TREE_OFFSET = 3;

  private final ServerConnection connection;

  // Hotbar slot 0: Food
  // Hotbar slot 1: Axe
  // Hotbar slot 2: Saplings

  private int slowDown = 0;

  private boolean movingBackwards = false;

  // TODO fix dropping logs wrong
  public OakFarm(ServerConnection connection) {
    this.connection = connection;
  }

  private Location location() {
    return connection.getPlayer().getLocation();
  }

  private void location(Location l2) {
    this.connection.getPlayer().setLocation(l2);
  }

  private int direction() {
    return movingBackwards ? -1 : 1;
  }

  private int length() {
    return FARM_END.select(ROW) - FARM_START.select(ROW);
  }

  private int width() {
    return FARM_END.select(COLUMN) - FARM_START.select(COLUMN);
  }

  private int row() {
    return location().toBlockLocation().select(ROW) - FARM_START.select(ROW);
  }

  private int col() {
    return location().toBlockLocation().select(COLUMN) - FARM_START.select(COLUMN);
  }

  public void start() {
    System.out.println("Started at " + location());

    connection.getBus().observe(BreakTooFastEvent.class)
        .doOnNext(event -> {
          System.out.println("Slowing down");
          slowDown++;
        })
        .subscribe();

    connection.sendPacket(new ServerboundSetCarriedItemPacket(1));

    BlockLocation block = location().toBlockLocation();
    if (!block.isAABB(FARM_START, FARM_END)) {
      // Outside farm
      System.out.println("Outside farm");
      return;
    }

    // TODO it won't fully break the tree a player is standing in if they started breaking it,
    // get kicked and then rejoin, the bot just moves to the next tree. However, this fixes
    // itself the next time the bot comes around

    int rowOffset = row();
    int colOffset = col();
    if (colOffset >= 33) {
      colOffset--; // centre
    }

    this.movingBackwards = (colOffset / 5) % 2 == 1;
    if (rowOffset < 0 || rowOffset > length()) {
      // Outside farm
      System.out.println("Impossibly outside of farm");
      return;
    } else if (rowOffset == 0) {
      // At home row
      if (colOffset == 0) {
        init().andThen(alignMiddleRowForwards()).subscribe();
      } else {
        harvestFromStart().subscribe();
      }
    } else if (rowOffset == length()) {
      // At end row
      harvestFromEnd().subscribe();
    } else {
      // In middle row
      if (colOffset % 10 == 0) {
        alignMiddleRowForwards().subscribe();
      } else if (colOffset % 10 == 5) {
        this.movingBackwards = true;
        alignMiddleRowBackwards().subscribe();
      } else {
        System.out.println("Not in valid column");
      }
    }
  }

  private Completable init() {
    return this.connection.getHeartbeat()
        .queue(new UntilAction(() -> this.connection.getPlayer().getInventory().isInitialized()))
        .andThen(connection.getHeartbeat().waitTicks(5))
        .andThen(Completable.fromAction(() -> {
          location(location().setYaw(-130).setPitch(47));
          System.out.println("Rotating");
          Location loc = location();
          this.connection.sendPacket(
              new ServerboundMovePlayerPosRotPacket(true, loc.getX(), loc.getY(), loc.getZ(),
                  loc.getYaw(), loc.getPitch()));
        }))
        .andThen(connection.getHeartbeat().waitTicks(4))
        .andThen(Completable.fromAction(() -> {
          System.out.println("Dropping");
          drop();
        }))
        .andThen(connection.getHeartbeat().waitTicks(4))
        .andThen(Completable.fromAction(() -> {
          location(location().setYaw(45).setPitch(30));
          this.connection.sendPacket(new ServerboundMovePlayerRotPacket(true, 45, 30));
        }))
        .andThen(connection.getHeartbeat().waitTicks(20))
        .andThen(Completable.fromAction(() -> {
          BlockLocation saplingChest = FARM_START.subtract(COLUMN).subtract(ROW);
          this.connection.sendPacket(
              new ServerboundUseItemOnPacket(saplingChest.toProtocolPosition(), Direction.UP,
                  Hand.MAIN_HAND, 0.5f, 0.5f, 0.5f, false));
        }))
        .andThen(this.connection.getHeartbeat().queue(new UntilAction(() -> {
          Inventory openInventory = this.connection.getPlayer().getOpenInventory();
          return openInventory != null && openInventory.isInitialized();
        })))
        .andThen(Completable.defer(() -> {
          Player player = this.connection.getPlayer();
          Inventory inventory = player.getOpenInventory();
          int counter = inventory.getCounter();

          Completable completable = Completable.complete();
          int saplingCount = countSaplings();
          if (hasSaplingSpace()) {
            // Look through chest slots
            for (int i = 0; i <= 53; i++) {
              ItemStack item = inventory.getItems()[i];
              if (item != null && item.getId() == SAPLING_ITEM) {
                // Take birch saplings
                ServerboundContainerClickPacket packet = new ServerboundContainerClickPacket(inventory.getId(),
                    counter++, i,
                    ContainerActionType.SHIFT_CLICK_ITEM, ShiftClickItemAction.LEFT_CLICK, item, Collections.emptyMap());
                completable = completable
                    .andThen(Completable.fromAction(() -> this.connection.sendPacket(packet))
                        .andThen(connection.getHeartbeat().waitTicks(5)));

                saplingCount += item.getAmount();
                if (saplingCount >= 64 * 7) {
                  System.out.println("Have enough saplings: " + saplingCount);
                  break;
                }
              }
            }
          }
          System.out.println("Starting with " + saplingCount + " saplings.");

          this.movingBackwards = false;
          inventory.setCounter(counter);
          return completable.andThen(Completable.fromAction(
              () -> {
                this.connection.sendPacket(new ServerboundContainerClosePacket(inventory.getId()));
                player.setOpenInventory(null);
              }));
        }))
        .andThen(connection.getHeartbeat().waitTicks(5))
        .andThen(Completable.defer(() -> {
          ItemStack axe = connection.getPlayer().getInventory().getItems()[37];
          if (isGoodAxe(axe)) {
            return Completable.complete();
          }

          System.out.println("Replacing axe");
          return swapOutAxe();
        }));
  }

  private Completable swapOutAxe() {
    return Completable.defer(() -> {
      // Opens chest and swaps out axe for a new one
      BlockLocation chest = FARM_START.subtract(ROW.multiply(2));

      location(location().setYaw(90).setPitch(20));
//      connection.updateLocation();
      Location loc = connection.getPlayer().getLocation();
      this.connection.sendPacket(new ServerboundMovePlayerPosRotPacket(true, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));
      this.connection.sendPacket(
          new ServerboundUseItemOnPacket(chest.toProtocolPosition(), Direction.UP,
              Hand.MAIN_HAND, 0.5f, 0.5f, 0.5f, false));

      return this.connection.getHeartbeat()
          .queue(new UntilAction(() -> {
            Inventory openInventory = this.connection.getPlayer().getOpenInventory();
            return openInventory != null && openInventory.isInitialized();
          }))
          .andThen(this.connection.getHeartbeat().waitTicks(1))
          .andThen(Completable.defer(() -> {
            System.out.println("Axe chest opened");
            Player player = this.connection.getPlayer();
            Inventory inventory = player.getOpenInventory();
            PlayerInventory playerInventory = player.getInventory();
            int counter = inventory.getCounter();
            // Insert old axe

            ItemStack oldAxe = playerInventory.getItems()[37];
            if (oldAxe != null) {
              this.connection.sendPacket(new ServerboundContainerClickPacket(0, counter++, 37 + 18,
                  ContainerActionType.SHIFT_CLICK_ITEM, ShiftClickItemAction.LEFT_CLICK, oldAxe, Collections.emptyMap()));
            }

            // Retrieve new axe
            ItemStack[] items = inventory.getItems();
            boolean retrieved = false;
            for (int i = 0; i < items.length; i++) {
              ItemStack item = items[i];
              if (isGoodAxe(item)) {
                this.connection.sendPacket(
                    new ServerboundContainerClickPacket(inventory.getId(), counter++, i,
                        ContainerActionType.MOVE_TO_HOTBAR_SLOT, MoveToHotbarAction.SLOT_2, item, Collections.emptyMap()));
                retrieved = true;
                break;
              }
            }
            inventory.setCounter(counter);

            this.connection.sendPacket(new ServerboundContainerClosePacket(inventory.getId()));
            player.setOpenInventory(null);
            if (retrieved) {
              return Completable.complete();
            } else {
              return this.connection.getHeartbeat().waitTicks(1)
                  .doOnComplete(() -> this.connection.disconnected("No axes left"));
            }
          }));
    });
  }

  private boolean hasSaplingSpace() {
    PlayerInventory inventory = connection.getPlayer().getInventory();
    for (int hotbar = 2 + 36; hotbar < 9 + 36; hotbar++) {
      if (inventory.getItems()[hotbar] == null) {
        return true;
      }
    }
    return false;
  }

  private int countSaplings() {
    int count = 0;
    PlayerInventory inventory = connection.getPlayer().getInventory();
    for (int hotbar = 2 + 36; hotbar < 9 + 36; hotbar++) {
      ItemStack item = inventory.getItems()[hotbar];
      if (item != null && item.getId() == 25) {
        count += item.getAmount();
      }
    }
    return count;
  }

  private Completable harvestFromStart() {
    return Completable
        .defer(() -> {
          this.movingBackwards = false;
          if (col() >= width()) {
            return connection.getHeartbeat()
                .queue(new MoveToAction(connection,
                    FARM_START.centre()))
                .andThen(connection.getHeartbeat().waitTicks(20))
                .doOnComplete(() -> this.connection.disconnected("Oak farm complete"));
          } else {
            return Completable
                .defer(() -> {
                  Location location = location();
                  int col = col();
                  // 33 is centre column
                  if (col >= 33) {
                    col--;
                  }
                  int colmod = col % 10;
                  if (colmod == 0) {
                    return Completable.complete();
                  } else {
                    // Line going through the centre of the farm
                    int of = 10 - colmod;

                    // centre column
                    if (col < 33 && col + of >= 33) {
                      ++of;
                    }
                    return connection.getHeartbeat().queue(
                        new MoveToAction(connection,
                            location.toBlockLocation().add(COLUMN.multiply(of)).centre()));
                  }
                })
                .andThen(alignMiddleRowForwards());
          }
        });
  }

  private Completable harvestFromEnd() {
    return Completable
        .defer(() -> {
          Location location = location();
          int col = col();
          // Load the hopper chunks
          // Number depends on how big the farm is
          Completable completable = Completable.complete();
          /*if (col < 19 + 16) {
            completable = connection.getHeartbeat().queue(new MoveToAction(connection,
                location.toBlockLocation().add(COLUMN.multiply((25+16) - col)).centre()))
                .andThen(connection.getHeartbeat().waitTicks(600));
          }*/
          if (col >= 33) {
            col--; // centre
          }

          int m10 = col % 10;

          int destcol;
          if (m10 <= 5) {
            destcol = 5 - m10;
          } else {
            destcol = 10 - m10;
          }

          // Line going through the centre of the farm
          if (col < 33 && col + destcol >= 33) {
            ++destcol; // centre
          }

          return completable.andThen(connection.getHeartbeat().queue(new MoveToAction(connection,
              location.toBlockLocation().add(COLUMN.multiply(destcol)).centre())));
        })
        .andThen(Completable.fromAction(() -> this.movingBackwards = true))
        .andThen(alignMiddleRowBackwards());
  }

  private Completable alignMiddleRowBackwards() {
    return Completable.defer(() -> {
      BlockLocation start = location().toBlockLocation();
      int row = row();
      if (row <= 4) {
        // go to start row
        return breakNBlocksAhead(false, start, row)
            .andThen(Completable.defer(() -> {
              if (location().toBlockLocation().select(COLUMN) == FARM_END.select(COLUMN)) {
                // wait for hoppers etc
                return connection.getHeartbeat().waitTicks(850);
              } else {
                return Completable.complete();
              }
            }))
            .andThen(harvestFromStart());
      }

      int offset;
      if (row > length() - FIRST_TREE_OFFSET) {
        offset = FIRST_TREE_OFFSET - (length() - row) - 1;
      } else {
        int mod5 = (row + 1 + (4 - FIRST_TREE_OFFSET)) % 5;
        if (mod5 == 0) {
          // Under a tree
          offset = 4;
        } else {
          offset = mod5 - 1;
        }
      }
      System.out.println("Moving backward " + offset + " blocks");

      Location l = location().setYaw(90).setPitch(0);
      location(l);
      connection.sendPacket(new ServerboundMovePlayerPosRotPacket(true, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch()));

      return breakNBlocksAhead(false, start, offset)
          .andThen(breakTree());
    });
  }

  private Completable alignMiddleRowForwards() {
    return Completable.defer(() -> {
      BlockLocation start = location().toBlockLocation();
      int row = row();
      if (row >= length() - 4) {
        // go to end row
        int offset = length() - row;
        return breakNBlocksAhead(true, start, offset).andThen(harvestFromEnd());
      }

      int offset;
      if (row < FIRST_TREE_OFFSET) {
        // The first tree is three blocks out from the start, so go one block before that
        offset = (FIRST_TREE_OFFSET - 1) - row;
      } else {
        int mod5 = (row+(4-FIRST_TREE_OFFSET)) % 5;
        if (mod5 == 4) {
          // We are standing under a tree
          offset = 4;
        } else {
          // Go to just before the next tree
          offset = 3 - mod5;
        }
      }
      System.out.println("Moving forward " + offset + " blocks");

      Location l = location().setYaw(-90).setPitch(0);
      location(l);
      connection.sendPacket(new ServerboundMovePlayerPosRotPacket(true, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch()));

      return breakNBlocksAhead(true, start, offset).andThen(connection.getHeartbeat().waitTicks(5)).andThen(breakTree());
    });
  }

  private Completable breakNBlocksAhead(boolean ahead, BlockLocation start, int offset) {
    Completable completable = Completable.complete();
    for (int i = 1; i <= offset; i++) {
      completable = completable.andThen(breakAheadBlock(ahead))
          .andThen(connection.getHeartbeat()
              .queue(new MoveToAction(connection, start.add(ROW.multiply(ahead ? i : -i)).centre()))
              .timeout(3, TimeUnit.SECONDS)
              .onErrorResumeWith(Completable.defer(() -> {
                System.out.println("error, breaking ahead block retrying");
                return this.breakAheadBlock(ahead);
              })));
    }
    return completable;
  }

  private Completable breakAheadBlock(boolean ahead) {
    // Break leaves at base of tree
    return Completable.defer(() -> {
      int x = ahead ? 1 : -1;
      Direction direction = ahead ? Direction.WEST : Direction.EAST;

      Location location = location();
      BlockLocation aheadBlock = location.toBlockLocation().add(x, 1, 0);

      connection.sendPacket(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING,
          aheadBlock.toProtocolPosition(), direction));
      connection.sendPacket(new ServerboundPlayerActionPacket(PlayerAction.FINISH_DIGGING,
          aheadBlock.toProtocolPosition(), direction));

      return connection.getHeartbeat().waitTicks(11 + slowDown);
    });
  }

  private int getBreakDuration(int efficiency) {
    if (efficiency == 0) {
      return 26 + slowDown;
    } else {
      return 8 - efficiency + 8 + slowDown;
    }
  }

  private Completable breakTree() {
    return this.connection.getHeartbeat()
        .queue(new UntilAction(() -> this.connection.getPlayer().getInventory().isInitialized()))
        .andThen(Completable.fromAction(() -> {
          Location location = location();
          location(location.setYaw(-90 * direction()).setPitch(70));
          Location l = location();
          connection.sendPacket(new ServerboundMovePlayerPosRotPacket(true, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch()));
        }))
        .andThen(connection.getHeartbeat().waitTicks(2))
        .andThen(Completable.defer(() -> {
          connection.sendPacket(new ServerboundSetCarriedItemPacket(1));

          BlockLocation block = location().toBlockLocation();
          BlockLocation break1 = block.add(ROW.multiply(direction()));
          BlockLocation break2 = break1.add(0, 1, 0);

          ItemStack axe = connection.getPlayer().getInventory().getItems()[37];
          if (axe == null || axe.getId() != AXE_ITEM) {
            System.out.println("No axe, aborting");
            this.connection.disconnected("No axe, aborting");
            return Completable.complete();
          }

          int efficiency = getEfficiency(axe);

          Direction face = movingBackwards ? Direction.EAST : Direction.WEST;
          System.out.println("Breaking base: " + break1);

          connection.sendPacket(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING,
              break1.toProtocolPosition(), face));
          connection.sendPacket(new ServerboundPlayerActionPacket(PlayerAction.FINISH_DIGGING,
              break1.toProtocolPosition(), face));
          return connection.getHeartbeat().waitTicks(getBreakDuration(efficiency))
              .andThen(Completable.fromAction(() -> {
                Location l3 = location();
                location(l3.setYaw(-90 * direction()).setPitch(0));
                Location l = location();
                connection.sendPacket(new ServerboundMovePlayerPosRotPacket(true, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch()));
              }))
              .andThen(connection.getHeartbeat().waitTicks(2))
              .andThen(Completable.fromAction(() -> {
                connection.sendPacket(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING,
                    break2.toProtocolPosition(), face));
                connection.sendPacket(new ServerboundPlayerActionPacket(PlayerAction.FINISH_DIGGING,
                    break2.toProtocolPosition(), face));
              }))
              .andThen(connection.getHeartbeat().waitTicks(getBreakDuration(efficiency)))
              .andThen(
                  connection.getHeartbeat().queue(new MoveToAction(connection, break1.centre()))
                      .timeout(3, TimeUnit.SECONDS))
              .andThen(Completable.defer(() -> {
                Location l2 = location();
                location(l2.setYaw(-90 * direction()).setPitch(-90));
                Location l = location();
                connection.sendPacket(new ServerboundMovePlayerPosRotPacket(true, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch()));

                Completable completable = connection.getHeartbeat().waitTicks(2);
                int maxTreeHeight = 6;
                for (int i = 2; i < maxTreeHeight; i++) {
                  Position position = break1.add(0, i, 0).toProtocolPosition();
                  completable = completable
                      .andThen(Completable.fromAction(() -> {
                        connection
                            .sendPacket(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING,
                                position, Direction.DOWN));
                        connection
                            .sendPacket(new ServerboundPlayerActionPacket(PlayerAction.FINISH_DIGGING,
                                position, Direction.DOWN));
                      }))
                      .andThen(connection.getHeartbeat().waitTicks(getBreakDuration(efficiency)));
                }
                return completable;
              }));
        }))
        .andThen(Completable.defer(() -> {
          if (this.connection.getPlayer().getFood() <= 15) {
            this.connection.sendPacket(new ServerboundSetCarriedItemPacket(0));
            this.connection.sendPacket(new ServerboundUseItemPacket(Hand.MAIN_HAND));
            return this.connection.getBus().observe(FinishUseItemEvent.class)
                .timeout(5, TimeUnit.SECONDS)
                .firstOrError()
                .flatMapCompletable(e -> Completable.fromAction(() -> this.connection.sendPacket(
                    new ServerboundPlayerActionPacket(PlayerAction.RELEASE_USE_ITEM,
                        new Position(0, 0, 0), Direction.DOWN))));
          } else {
            return Completable.complete();
          }
        }))
        .andThen(plantSapling())
        .andThen(dropItems())
        .andThen(connection.getHeartbeat().waitTicks(15))
        .andThen(Completable.defer(() -> {
          PlayerInventory inventory = connection.getPlayer().getInventory();
          ItemStack axe = inventory.getItems()[37];
          if (!isGoodAxe(axe)) {
            return moveToStart();
          } else if (movingBackwards) {
            return alignMiddleRowBackwards();
          } else {
            return alignMiddleRowForwards();
          }
        }))
        .doOnError(Throwable::printStackTrace)
        .onErrorResumeWith(Completable.defer(() -> {
          System.out.println("error, retrying");
          return this.breakTree();
        }));
  }

  private Completable dropItems() {
    return Completable.defer(() -> {
      if (row() != FIRST_TREE_OFFSET) {
        return Completable.complete();
      } else {
        return Completable
            .fromAction(() -> {
              location(location().setYaw(180).setPitch(30));
              Location l = location();
              connection.sendPacket(new ServerboundMovePlayerPosRotPacket(true, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch()));
            })
            .andThen(connection.getHeartbeat().waitTicks(1))
            .andThen(Completable.fromAction(() -> {
              System.out.println("Dropping in row");
              drop();
            }));
      }
    });
  }

  private void drop() {
    PlayerInventory inventory = this.connection.getPlayer().getInventory();
    ItemStack[] items = inventory.getItems();
    for (int i = 9; i <= 44; i++) {
      if (i == 36 || i == 37) {
        continue;
      }
      ItemStack item = items[i];
      if (item != null && (i <= 35 || item.getId() != SAPLING_ITEM)) {
        inventory.drop(i);
      }
    }
  }

  private Completable moveToStart() {
    return Completable.defer(() -> {
      int column = col() - 6;
      return Completable
          .defer(() -> {
            if (movingBackwards) {
              int moveback = length() - row();
              BlockLocation end = location().toBlockLocation().add(ROW.multiply(moveback));
              BlockLocation ocol = end.subtract(COLUMN.multiply(5));
              return connection.getHeartbeat().queue(new MoveToAction(connection, end.centre()))
                  .andThen(
                      connection.getHeartbeat().queue(new MoveToAction(connection, ocol.centre())))
                  .andThen(connection.getHeartbeat().queue(
                      new MoveToAction(connection, ocol.subtract(ROW.multiply(length())).centre())))
                  .andThen(
                      connection.getHeartbeat()
                          .queue(new MoveToAction(connection, FARM_START.centre())));
            } else {
              BlockLocation start = location().toBlockLocation().subtract(ROW.multiply(row()));
              return connection.getHeartbeat().queue(new MoveToAction(connection, start.centre()))
                  .andThen(
                      connection.getHeartbeat()
                          .queue(new MoveToAction(connection, FARM_START.centre())));
            }
          })
          .andThen(init())
          .andThen(connection.getHeartbeat().queue(new MoveToAction(connection, FARM_START.add(COLUMN.multiply(column)).centre())))
          .andThen(harvestFromStart());
    });
  }

  private Completable plantSapling() {
    return Completable.fromAction(() -> {
      PlayerInventory inventory = connection.getPlayer().getInventory();
      int slot = 0;
      for (int i = 38; i <= 44; i++) {
        ItemStack item = inventory.getItems()[i];
        if (item != null && item.getId() == SAPLING_ITEM) {
          slot = i - 36;
        }
      }
      if (slot == 0) {
        System.out.println("No sapling");
        this.connection.disconnected("No saplings big problem");
        return;
      }
      this.connection.sendPacket(new ServerboundSetCarriedItemPacket(slot));
      Location l2 = location();
      location(l2.setYaw(-90 * direction()).setPitch(90f));
      Location l = location();
      connection.sendPacket(new ServerboundMovePlayerPosRotPacket(true, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch()));
      this.connection.sendPacket(
          new ServerboundUseItemOnPacket(
              location().toBlockLocation().add(0, -1, 0).toProtocolPosition(),
              Direction.UP, Hand.MAIN_HAND, 0.5f, 1, 0.5f, false));
      this.connection.sendPacket(new ServerboundUseItemPacket(Hand.MAIN_HAND));
    });
  }

  private boolean isGoodAxe(ItemStack axe) {
    if (axe == null || axe.getId() != AXE_ITEM) {
      return false;
    }

    CompoundTag nbt = axe.getNbt();
    if (nbt == null || !nbt.contains("Damage")) {
      return true;
    }

    int damage = nbt.<IntTag>get("Damage").getValue();
    return damage < 1561 - 16;
  }

  private int getEfficiency(ItemStack axe) {
    if (axe.getNbt() != null) {
      ListTag enchants = axe.getNbt().get("Enchantments");
      if (enchants != null) {
        for (int i = 0; i < enchants.size(); i++) {
          CompoundTag enchant = enchants.get(i);
          StringTag id = enchant.get("id");
          if (id.getValue().equals("minecraft:efficiency")) {
            ShortTag lvl = enchant.get("lvl");
            return lvl.getValue();
          }
        }
      }
  }
    return 0;
  }
}
