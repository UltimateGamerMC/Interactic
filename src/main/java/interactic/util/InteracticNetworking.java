package interactic.util;

import interactic.InteracticInit;
import interactic.ItemFilterItem;
import interactic.ItemFilterScreen;
import interactic.ItemFilterScreenHandler;
import interactic.mixin.ItemEntityAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class InteracticNetworking {

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(Pickup.TYPE, Pickup.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DropWithPower.TYPE, DropWithPower.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FilterModeRequest.TYPE, FilterModeRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ItemFilterItem.SetFilterModePacket.TYPE, ItemFilterItem.SetFilterModePacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Pickup.TYPE, (message, context) -> {
            var player = context.player();
            final var item = Helpers.raycastItem(player.getCamera(), 6);
            if (item == null || ((ItemEntityAccessor) item).interactic$getPickupDelay() == Short.MAX_VALUE) {
                return;
            }

            if (player.getInventory().add(item.getItem().copy())) {
                player.take(item, item.getItem().getCount());
                item.discard();
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(DropWithPower.TYPE, (message, context) -> {
            var player = context.player();
            var ext = (InteracticPlayerExtension) player;
            ext.setDropPower(message.power());
            ext.setDropDirection(message.pitch(), message.yaw());

            int count = message.dropAll() && !player.getInventory().getSelectedItem().isEmpty() ? player.getInventory().getSelectedItem().getCount() : 1;
            ItemStack removed = player.getInventory().removeItem(player.getInventory().getSelectedSlot(), count);
            if (!removed.isEmpty()) {
                player.drop(removed, true, Prediction.PREDICTED);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(FilterModeRequest.TYPE, (message, context) -> {
            if (!(context.player().containerMenu instanceof ItemFilterScreenHandler filterHandler)) return;
            filterHandler.setFilterMode(message.newMode());
        });
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(ItemFilterItem.SetFilterModePacket.TYPE, (message, context) -> {
            if (!(Minecraft.getInstance().gui.screen() instanceof ItemFilterScreen screen)) return;
            screen.blockMode = message.mode();
        });
    }

    /** Sends to the server if it has Interactic installed; silently does nothing otherwise. */
    @Environment(EnvType.CLIENT)
    public static void sendToServer(CustomPacketPayload payload) {
        if (ClientPlayNetworking.canSend(payload.type())) {
            ClientPlayNetworking.send(payload);
        }
    }

    public static void sendToPlayer(Player player, CustomPacketPayload payload) {
        if (player instanceof ServerPlayer serverPlayer && ServerPlayNetworking.canSend(serverPlayer, payload.type())) {
            ServerPlayNetworking.send(serverPlayer, payload);
        }
    }

    public record Pickup() implements CustomPacketPayload {
        public static final Type<Pickup> TYPE = new Type<>(InteracticInit.id("pickup"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Pickup> CODEC = StreamCodec.unit(new Pickup());

        @Override
        public Type<Pickup> type() {
            return TYPE;
        }
    }

    public record DropWithPower(float power, boolean dropAll, float pitch, float yaw) implements CustomPacketPayload {
        public static final Type<DropWithPower> TYPE = new Type<>(InteracticInit.id("drop_with_power"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DropWithPower> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, DropWithPower::power,
            ByteBufCodecs.BOOL, DropWithPower::dropAll,
            ByteBufCodecs.FLOAT, DropWithPower::pitch,
            ByteBufCodecs.FLOAT, DropWithPower::yaw,
            DropWithPower::new
        );

        @Override
        public Type<DropWithPower> type() {
            return TYPE;
        }
    }

    public record FilterModeRequest(boolean newMode) implements CustomPacketPayload {
        public static final Type<FilterModeRequest> TYPE = new Type<>(InteracticInit.id("filter_mode_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FilterModeRequest> CODEC = ByteBufCodecs.BOOL.map(FilterModeRequest::new, FilterModeRequest::newMode).cast();

        @Override
        public Type<FilterModeRequest> type() {
            return TYPE;
        }
    }
}
