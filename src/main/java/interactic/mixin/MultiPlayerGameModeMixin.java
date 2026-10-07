package interactic.mixin;

import interactic.InteracticInit;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Redirect(method = "dropItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z"))
    private boolean interactic$dontSwingArms(LocalPlayer player, InteractionHand hand, SwingAnimation animation, boolean sendToSwingingEntity) {
        if (!InteracticInit.getConfig().swingArm()) return false;
        return player.swing(hand, animation, sendToSwingingEntity);
    }
}
