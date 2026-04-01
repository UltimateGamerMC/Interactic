package interactic;

import interactic.util.InteracticNetworking;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ItemFilterScreenHandler extends AbstractContainerMenu {

    public static final int SLOT_COUNT = 27;
    private final Container inventory;
    private final Player player;

    public ItemFilterScreenHandler(int syncId, net.minecraft.world.entity.player.Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(SLOT_COUNT));
    }

    public ItemFilterScreenHandler(int syncId, net.minecraft.world.entity.player.Inventory playerInventory, Container inventory) {
        super(InteracticInit.ITEM_FILTER_SCREEN_HANDLER, syncId);
        this.inventory = inventory;
        checkContainerSize(inventory, SLOT_COUNT);
        this.player = playerInventory.player;
        inventory.startOpen(playerInventory.player);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = col + row * 9;
                this.addSlot(new GhostSlot(inventory, slot, 8 + col * 18, 20 + row * 18));
            }
        }
        this.addStandardInventorySlots(playerInventory, 8, 96);
    }

    public void setFilterMode(boolean mode) {
        if (!(inventory instanceof ItemFilterItem.FilterInventory filterInventory)) return;
        filterInventory.setFilterMode(mode);
        InteracticNetworking.CHANNEL.serverHandle(player).send(new ItemFilterItem.SetFilterModePacket(mode));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
    }

    private static class GhostSlot extends Slot {

        GhostSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }

        @Override
        public void setByPlayer(ItemStack stack, ItemStack previous) {
            if (!stack.isEmpty()) {
                this.set(new ItemStack(stack.getItem(), 1));
            } else {
                this.set(ItemStack.EMPTY);
            }
        }

        @Override
        public boolean mayPickup(Player player) {
            this.set(ItemStack.EMPTY);
            return false;
        }

        @Override
        public boolean isFake() {
            return true;
        }
    }
}
