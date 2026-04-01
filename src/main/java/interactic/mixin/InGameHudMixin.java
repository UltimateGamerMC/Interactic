package interactic.mixin;

import interactic.InteracticInit;
import interactic.util.Helpers;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Gui.class)
public class InGameHudMixin {

    @Inject(method = "extractCrosshair", at = @At("TAIL"))
    private void interactic$renderItemTooltip(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!InteracticInit.getConfig().renderItemTooltips()) return;

        final var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        final var item = Helpers.raycastItem(client.getCameraEntity(), 5);

        if (item == null) return;
        var ctx = Item.TooltipContext.of(client.level);
        var stack = item.getItem();
        var tooltip = InteracticInit.getConfig().renderFullTooltip()
                ? stack.getTooltipLines(ctx, client.player, TooltipFlag.Default.NORMAL)
                : List.of(stack.getHoverName());

        for (int i = 0, tooltipSize = tooltip.size(); i < tooltipSize; i++) {
            final var text = tooltip.get(i);
            graphics.text(client.font, text, graphics.guiWidth() / 2 - client.font.width(text) / 2, graphics.guiHeight() / 2 + 15 + i * 10, 0xFFFFFF, true);
        }
    }
}
