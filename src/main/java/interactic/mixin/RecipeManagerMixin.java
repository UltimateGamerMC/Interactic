package interactic.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import interactic.InteracticInit;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.SortedMap;

@Mixin(ServerRecipeManager.class)
public class RecipeManagerMixin {

    @Shadow
    private RegistryWrapper.WrapperLookup registries;

    private static final String FILTER_RECIPE = """
            {
                "type": "minecraft:crafting_shaped",
                "pattern": [
                    " c ",
                    "cEc",
                    " c "
                ],
                "key": {
                    "c": {
                        "item": "minecraft:copper_ingot"
                    },
                    "E": {
                        "item": "minecraft:ender_pearl"
                    }
                },
                "result": {
                    "id": "interactic:item_filter",
                    "count": 1
                }
            }
            """;

    @Inject(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/JsonDataLoader;load(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/resource/ResourceFinder;Lcom/mojang/serialization/DynamicOps;Lcom/mojang/serialization/Codec;Ljava/util/Map;)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectFilterRecipe(ResourceManager resourceManager, Profiler profiler, CallbackInfoReturnable cir, SortedMap<Identifier, Recipe<?>> sortedMap) {
        if (!InteracticInit.getConfig().itemFilterEnabled()) return;
        Recipe.CODEC.parse(registries.getOps(JsonOps.INSTANCE), new Gson().fromJson(FILTER_RECIPE, JsonElement.class))
                .result()
                .ifPresent(recipe -> sortedMap.put(InteracticInit.id("item_filter"), recipe));
    }
}
