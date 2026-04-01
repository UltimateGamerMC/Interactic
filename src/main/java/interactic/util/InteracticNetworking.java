package interactic.util;

import interactic.InteracticInit;
import interactic.ItemFilterItem;
import interactic.ItemFilterScreen;
import interactic.ItemFilterScreenHandler;
import interactic.mixin.ItemEntityAccessor;
import io.wispforest.owo.network.OwoNetChannel;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
public class InteracticNetworking {

    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(InteracticInit.id("channel"));

    public static void init() {
        CHANNEL.registerClientboundDeferred(ItemFilterItem.SetFilterModePacket.class);

        CHANNEL.registerServerbound(Pickup.class, (message, access) -> {
            final var item = Helpers.raycastItem(access.player().getCamera(), 6);
            if (item == null || ((ItemEntityAccessor) item).interactic$getPickupDelay() == Short.MAX_VALUE) {
                return;
            }

            if (access.player().getInventory().add(item.getItem().copy())) {
                access.player().take(item, item.getItem().getCount());
                item.discard();
            }
        });

        CHANNEL.registerServerbound(DropWithPower.class, (message, access) -> {
            var ext = (InteracticPlayerExtension) access.player();
            ext.setDropPower(message.power);
            ext.setDropDirection(message.pitch, message.yaw);

            var player = access.player();
            int count = message.dropAll && !player.getInventory().getSelectedItem().isEmpty() ? player.getInventory().getSelectedItem().getCount() : 1;
            ItemStack removed = player.getInventory().removeItem(player.getInventory().getSelectedSlot(), count);
            if (!removed.isEmpty()) {
                player.drop(removed, false, true);
            }
        });

        CHANNEL.registerServerbound(FilterModeRequest.class, (message, access) -> {
            if (!(access.player().containerMenu instanceof ItemFilterScreenHandler filterHandler)) return;
            filterHandler.setFilterMode(message.newMode);
        });
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        CHANNEL.registerClientbound(ItemFilterItem.SetFilterModePacket.class, (message, access) -> {
            if (!(Minecraft.getInstance().screen instanceof ItemFilterScreen screen)) return;
            screen.blockMode = message.mode();
        });
    }

    public record Pickup() {}

    public record DropWithPower(float power, boolean dropAll, float pitch, float yaw) {}

    public record FilterModeRequest(boolean newMode) {}
}
