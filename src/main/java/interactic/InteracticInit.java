package interactic;

import interactic.util.InteracticConfig;
import interactic.util.InteracticNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;


public class InteracticInit implements ModInitializer {

    public static final String MOD_ID = "interactic";

    private static Item ITEM_FILTER = null;

    private static final InteracticConfig CONFIG = InteracticConfig.createAndLoad();
    private static float itemRotationSpeedMultiplier = 1f;

    public static final MenuType<ItemFilterScreenHandler> ITEM_FILTER_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, id("item_filter"), new MenuType<>(ItemFilterScreenHandler::new, FeatureFlags.VANILLA_SET));

    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().isModLoaded("iris")) itemRotationSpeedMultiplier = 0.5f;

        if (CONFIG.itemFilterEnabled()) {
            ResourceKey<Item> itemFilterKey = ResourceKey.create(Registries.ITEM, id("item_filter"));
            ITEM_FILTER = Registry.register(BuiltInRegistries.ITEM, itemFilterKey, new ItemFilterItem(itemFilterKey));
        }

        InteracticNetworking.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Item getItemFilter() {
        return ITEM_FILTER == null ? Items.AIR : ITEM_FILTER;
    }

    public static float getItemRotationSpeedMultiplier() {
        return itemRotationSpeedMultiplier;
    }

    public static InteracticConfig getConfig() {
        return CONFIG;
    }
}
