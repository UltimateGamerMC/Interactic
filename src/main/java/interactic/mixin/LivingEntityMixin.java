package interactic.mixin;

import interactic.InteracticInit;
import interactic.util.InteracticItemExtensions;
import interactic.util.InteracticPlayerExtension;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "createItemEntity(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemEntity;setVelocity(DDD)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    private void applyDropPower(ItemStack stack, boolean atSelf, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir, double d, ItemEntity item) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof PlayerEntity player) {
            item.setYaw(player.getYaw());
            item.setPitch(player.getPitch());
        }
        if (!(self instanceof InteracticPlayerExtension ext) || ext.getDropPower() <= 1 || !InteracticInit.getConfig().itemThrowing()) return;

        PlayerEntity player = (PlayerEntity) self;
        float power = ext.getDropPower();
        Vec3d dir = Float.isNaN(ext.getDropPitch()) || Float.isNaN(ext.getDropYaw())
                ? player.getRotationVec(0f)
                : Vec3d.fromPolar(ext.getDropPitch(), ext.getDropYaw());
        Vec3d normDir = dir.normalize();
        item.setVelocity(normDir.multiply(power * .35f));
        float rot = player.getYaw() * ((float) Math.PI / 180f) + ((float) Math.PI / 4f);
        ((InteracticItemExtensions) item).setRotation(rot);
        ext.setDropDirection(Float.NaN, Float.NaN);
        item.velocityDirty = true;
        item.updatePosition(item.getX(), player.getEyeY(), item.getZ());
        if (retainOwnership) {
            ((InteracticItemExtensions) item).markThrown();
            if (power >= 5) ((InteracticItemExtensions) item).markFullPower();
        }
        ext.setDropPower(1);
    }
}
