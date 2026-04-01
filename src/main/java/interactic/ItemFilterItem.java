package interactic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import interactic.util.InteracticNetworking;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ItemFilterItem extends Item {

    private static final StreamCodec<RegistryFriendlyByteBuf, NonNullList<ItemStack>> FILTER_SLOTS_STREAM_CODEC = StreamCodec.of(
            (buf, list) -> {
                for (int i = 0; i < ItemFilterScreenHandler.SLOT_COUNT; i++) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, list.get(i));
                }
            },
            buf -> {
                NonNullList<ItemStack> list = NonNullList.withSize(ItemFilterScreenHandler.SLOT_COUNT, ItemStack.EMPTY);
                for (int i = 0; i < ItemFilterScreenHandler.SLOT_COUNT; i++) {
                    list.set(i, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                }
                return list;
            }
    );

    public static final Codec<NonNullList<ItemStack>> FILTER_SLOTS_CODEC = InventoryEntry.LIST_CODEC;

    public static final DataComponentType<Boolean> ENABLED = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            InteracticInit.id("item_filter_enabled"),
            DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build()
    );

    public static final DataComponentType<Boolean> BLOCK_MODE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            InteracticInit.id("item_filter_block_mode"),
            DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build()
    );

    public static final DataComponentType<NonNullList<ItemStack>> FILTER_SLOTS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            InteracticInit.id("item_filter_slots"),
            DataComponentType.<NonNullList<ItemStack>>builder()
                    .persistent(FILTER_SLOTS_CODEC)
                    .networkSynchronized(FILTER_SLOTS_STREAM_CODEC)
                    .build()
    );

    public ItemFilterItem(ResourceKey<Item> registryKey) {
        super(new Properties()
                .setId(registryKey)
                .stacksTo(1)
                .component(ENABLED, true)
                .component(BLOCK_MODE, true)
                .component(FILTER_SLOTS, NonNullList.withSize(ItemFilterScreenHandler.SLOT_COUNT, ItemStack.EMPTY)));
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        final var playerStack = user.getItemInHand(hand);
        if (user.isShiftKeyDown()) {
            playerStack.update(ENABLED, false, enabled -> !enabled);
        } else {
            if (world.isClientSide()) return InteractionResult.SUCCESS;
            final var inv = new FilterInventory(playerStack);
            final var factory = new MenuProvider() {
                @Override
                public @NotNull AbstractContainerMenu createMenu(int syncId, Inventory playerInv, Player player) {
                    return new ItemFilterScreenHandler(syncId, playerInv, inv);
                }

                @Override
                public Component getDisplayName() {
                    return Component.translatable(ItemFilterItem.this.getDescriptionId());
                }
            };
            user.openMenu(factory);
            InteracticNetworking.CHANNEL.serverHandle(user).send(new SetFilterModePacket(inv.getFilterMode()));
        }
        return InteractionResult.SUCCESS;
    }

    public static List<Item> getItemsInFilter(ItemStack stack) {
        return stack.getOrDefault(FILTER_SLOTS, NonNullList.withSize(ItemFilterScreenHandler.SLOT_COUNT, ItemStack.EMPTY)).stream().map(ItemStack::getItem).toList();
    }

    public static class FilterInventory extends SimpleContainer {

        public final ItemStack filter;

        public FilterInventory(ItemStack filter) {
            super(ItemFilterScreenHandler.SLOT_COUNT);
            this.filter = filter;
            NonNullList<ItemStack> filterItems = filter.getOrDefault(FILTER_SLOTS, NonNullList.withSize(ItemFilterScreenHandler.SLOT_COUNT, ItemStack.EMPTY));
            for (int i = 0; i < filterItems.size(); i++) {
                this.setItem(i, filterItems.get(i).copy());
            }
        }

        public void setFilterMode(boolean mode) {
            this.filter.set(BLOCK_MODE, mode);
        }

        public boolean getFilterMode() {
            return this.filter.getOrDefault(BLOCK_MODE, false);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            NonNullList<ItemStack> copy = NonNullList.withSize(ItemFilterScreenHandler.SLOT_COUNT, ItemStack.EMPTY);
            for (int i = 0; i < getContainerSize(); i++) {
                copy.set(i, getItem(i).copy());
            }
            this.filter.set(FILTER_SLOTS, copy);
        }

        @Override
        public boolean stillValid(Player player) {
            return player.getInventory().contains(filter);
        }
    }

    public record InventoryEntry(ItemStack stack, int slot) {
        public static final Codec<InventoryEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("stack").forGetter(InventoryEntry::stack),
                Codec.INT.fieldOf("slot").forGetter(InventoryEntry::slot)
        ).apply(i, InventoryEntry::new));

        public static final Codec<NonNullList<ItemStack>> LIST_CODEC = Codec.list(CODEC).xmap(entries -> {
            NonNullList<ItemStack> list = NonNullList.withSize(ItemFilterScreenHandler.SLOT_COUNT, ItemStack.EMPTY);
            entries.forEach(entry -> list.set(entry.slot, entry.stack));
            return list;
        }, stacks -> {
            var entries = new ArrayList<InventoryEntry>();
            for (int i = 0; i < stacks.size(); i++) {
                if (stacks.get(i).isEmpty()) continue;
                entries.add(new InventoryEntry(stacks.get(i), i));
            }
            return entries;
        });
    }

    public record SetFilterModePacket(boolean mode) {}
}
