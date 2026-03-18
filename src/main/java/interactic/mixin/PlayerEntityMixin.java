package interactic.mixin;

import interactic.util.InteracticPlayerExtension;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements InteracticPlayerExtension {

    @Unique
    private float dropPower = 1;
    @Unique
    private float dropPitch = Float.NaN;
    @Unique
    private float dropYaw = Float.NaN;

    @Override
    public void setDropPower(float power) {
        this.dropPower = power;
    }

    @Override
    public float getDropPower() {
        return this.dropPower;
    }

    @Override
    public void setDropDirection(float pitch, float yaw) {
        this.dropPitch = pitch;
        this.dropYaw = yaw;
    }

    @Override
    public float getDropPitch() {
        return dropPitch;
    }

    @Override
    public float getDropYaw() {
        return dropYaw;
    }
}
