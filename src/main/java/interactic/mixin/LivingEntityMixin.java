package interactic.mixin;

import interactic.InteracticInit;
import interactic.util.InteracticItemExtensions;
import interactic.util.InteracticPlayerExtension;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "createItemStackToDrop", at = @At("RETURN"), cancellable = false)
    private void interactic$applyDropPower(ItemStack stack, boolean randomly, boolean thrownFromHand, CallbackInfoReturnable<ItemEntity> cir) {
        ItemEntity item = cir.getReturnValue();
        if (item == null) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player) {
            item.setYRot(player.getYRot());
            item.setXRot(player.getXRot());
        }
        if (!(self instanceof InteracticPlayerExtension ext) || ext.getDropPower() <= 1 || !InteracticInit.getConfig().itemThrowing()) return;

        Player player = (Player) self;
        float power = ext.getDropPower();
        Vec3 dir = Float.isNaN(ext.getDropPitch()) || Float.isNaN(ext.getDropYaw())
                ? player.getViewVector(0f)
                : Vec3.directionFromRotation(ext.getDropPitch(), ext.getDropYaw());
        Vec3 normDir = dir.normalize();
        item.setDeltaMovement(normDir.scale(power * .35f));
        float rot = player.getYRot() * ((float) Math.PI / 180f) + ((float) Math.PI / 4f);
        ((InteracticItemExtensions) item).setRotation(rot);
        ext.setDropDirection(Float.NaN, Float.NaN);
        item.setPos(item.getX(), player.getEyeY(), item.getZ());
        if (thrownFromHand) {
            ((InteracticItemExtensions) item).markThrown();
            if (power >= 5) ((InteracticItemExtensions) item).markFullPower();
        }
        ext.setDropPower(1);
    }
}
