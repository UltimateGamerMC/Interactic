package interactic.mixin;

import interactic.InteracticClientInit;
import interactic.InteracticInit;
import interactic.util.Helpers;
import interactic.util.InteracticNetworking;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Unique
    private float interactic$dropPower = 0.9f;
    @Unique
    private int interactic$chargeTicks = 0;

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    @Final
    public net.minecraft.client.Options options;

    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z", shift = At.Shift.AFTER), cancellable = true)
    private void interactic$tryPickupItem(CallbackInfo ci) {
        if (this.player == null || this.player.isHandsBusy()) return;
        if (!InteracticInit.getConfig().rightClickPickup()) return;
        if (KeyMappingHelper.getBoundKeyOf(InteracticClientInit.PICKUP_ITEM).getValue() != InputConstants.UNKNOWN.getValue()) return;
        if (Helpers.raycastItem(((Minecraft) (Object) this).getCameraEntity(), (float) this.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)) == null) return;
        InteracticNetworking.sendToServer(new InteracticNetworking.Pickup());
        this.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
        ci.cancel();
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void interactic$chargeDropPower(CallbackInfo ci) {
        if (!InteracticInit.getConfig().itemThrowing()) return;
        if (player == null || player.isSpectator()) return;
        if (this.options.keyDrop.isDown() && !((Minecraft) (Object) this).hasShiftDown()) {
            float prev = interactic$dropPower;
            interactic$dropPower += 0.075f;
            if (interactic$dropPower > 5) interactic$dropPower = 5;
            interactic$chargeTicks++;
            if (interactic$dropPower >= 1.5 && (prev < 1.5 || interactic$chargeTicks % 20 == 0))
                player.sendOverlayMessage(Component.literal("Power: " + BigDecimal.valueOf(Math.max(interactic$dropPower, 1)).setScale(1, RoundingMode.HALF_UP)));
        } else {
            interactic$chargeTicks = 0;
        }
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;dropItem(Lnet/minecraft/client/player/LocalPlayer;Z)V"))
    private void interactic$handleQuickDrop(MultiPlayerGameMode gameMode, LocalPlayer clientPlayer, boolean dropEntireStack) {
        if (InteracticInit.getConfig().itemThrowing() && !((Minecraft) (Object) this).hasShiftDown()) return;
        gameMode.dropItem(clientPlayer, dropEntireStack);
    }

    @Inject(method = "handleKeybinds", at = @At("RETURN"))
    private void interactic$afterDrop(CallbackInfo ci) {
        if (!InteracticInit.getConfig().itemThrowing()) return;
        if (player == null) return;

        if (interactic$dropPower > 0.9f && !this.options.keyDrop.isDown()) {
            final var dropAll = ((Minecraft) (Object) this).hasControlDown();

            if (interactic$dropPower >= 1.5) {
                float sentPower = interactic$dropPower;
                InteracticNetworking.sendToServer(new InteracticNetworking.DropWithPower(interactic$dropPower, dropAll, this.player.getXRot(), this.player.getYRot()));

                int count = dropAll && !this.player.getInventory().getSelectedItem().isEmpty() ? this.player.getInventory().getSelectedItem().getCount() : 1;
                ItemStack taken = this.player.getInventory().removeItem(this.player.getInventory().getSelectedSlot(), count);
                if (!taken.isEmpty()) {
                    if (InteracticInit.getConfig().swingArm()) this.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
                    player.sendOverlayMessage(Component.literal("Thrown at power: " + BigDecimal.valueOf(sentPower).setScale(1, RoundingMode.HALF_UP)));
                }
            } else {
                this.gameMode.dropItem(this.player, dropAll);
            }

            interactic$dropPower = 0.9f;
        }
    }
}
