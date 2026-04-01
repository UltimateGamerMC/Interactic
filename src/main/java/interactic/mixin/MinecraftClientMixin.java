package interactic.mixin;

import interactic.InteracticClientInit;
import interactic.InteracticInit;
import interactic.util.Helpers;
import interactic.util.InteracticNetworking;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
    public net.minecraft.world.entity.Entity cameraEntity;

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    @Final
    public net.minecraft.client.Options options;

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z", shift = At.Shift.AFTER), cancellable = true)
    private void interactic$tryPickupItem(CallbackInfo ci) {
        if (this.player == null || this.player.isHandsBusy()) return;
        if (!InteracticInit.getConfig().rightClickPickup()) return;
        if (KeyMappingHelper.getBoundKeyOf(InteracticClientInit.PICKUP_ITEM).getValue() != InputConstants.UNKNOWN.getValue()) return;
        if (Helpers.raycastItem(this.cameraEntity, (float) this.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)) == null) return;
        InteracticNetworking.CHANNEL.clientHandle().send(new InteracticNetworking.Pickup());
        this.player.swing(InteractionHand.MAIN_HAND);
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

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;drop(Z)Z"))
    private boolean interactic$handleQuickDrop(LocalPlayer clientPlayer, boolean dropEntireStack) {
        if (!InteracticInit.getConfig().itemThrowing()) return clientPlayer.drop(dropEntireStack);
        if (!((Minecraft) (Object) this).hasShiftDown()) return false;
        return clientPlayer.drop(dropEntireStack);
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"))
    private void interactic$dontSwingArms(LocalPlayer player, InteractionHand hand) {
        if (!InteracticInit.getConfig().swingArm()) return;
        player.swing(hand);
    }

    @Inject(method = "handleKeybinds", at = @At("RETURN"))
    private void interactic$afterDrop(CallbackInfo ci) {
        if (!InteracticInit.getConfig().itemThrowing()) return;
        if (player == null) return;

        if (interactic$dropPower > 0.9f && !this.options.keyDrop.isDown()) {
            final var dropAll = ((Minecraft) (Object) this).hasControlDown();

            if (interactic$dropPower >= 1.5) {
                float sentPower = interactic$dropPower;
                InteracticNetworking.CHANNEL.clientHandle().send(new InteracticNetworking.DropWithPower(interactic$dropPower, dropAll, this.player.getXRot(), this.player.getYRot()));

                int count = dropAll && !this.player.getInventory().getSelectedItem().isEmpty() ? this.player.getInventory().getSelectedItem().getCount() : 1;
                ItemStack taken = this.player.getInventory().removeItem(this.player.getInventory().getSelectedSlot(), count);
                if (!taken.isEmpty()) {
                    if (InteracticInit.getConfig().swingArm()) this.player.swing(InteractionHand.MAIN_HAND);
                    player.sendOverlayMessage(Component.literal("Thrown at power: " + BigDecimal.valueOf(sentPower).setScale(1, RoundingMode.HALF_UP)));
                }
            } else if (this.player.drop(dropAll)) {
                if (InteracticInit.getConfig().swingArm()) this.player.swing(InteractionHand.MAIN_HAND);
            }

            interactic$dropPower = 0.9f;
        }
    }
}
