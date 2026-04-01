package interactic;

import interactic.util.InteracticNetworking;
import io.wispforest.owo.config.ui.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import com.mojang.blaze3d.platform.InputConstants;

public class InteracticClientInit implements ClientModInitializer {

    public static final KeyMapping PICKUP_ITEM = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.interactic.pickup_item",
            InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC));

    @Override
    public void onInitializeClient() {
        MenuScreens.register(InteracticInit.ITEM_FILTER_SCREEN_HANDLER, ItemFilterScreen::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (PICKUP_ITEM.consumeClick()) {
                InteracticNetworking.CHANNEL.clientHandle().send(new InteracticNetworking.Pickup());
                if (client.player != null) {
                    client.player.swing(InteractionHand.MAIN_HAND);
                }
            }
        });

        InteracticNetworking.initClient();
    }
}
