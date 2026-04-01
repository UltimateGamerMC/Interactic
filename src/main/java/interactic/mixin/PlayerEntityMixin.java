package interactic.mixin;

import interactic.util.InteracticPlayerExtension;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public class PlayerEntityMixin implements InteracticPlayerExtension {

    @Unique
    private float interactic$dropPower = 1;
    @Unique
    private float interactic$dropPitch = Float.NaN;
    @Unique
    private float interactic$dropYaw = Float.NaN;

    @Override
    public void setDropPower(float power) {
        this.interactic$dropPower = power;
    }

    @Override
    public float getDropPower() {
        return this.interactic$dropPower;
    }

    @Override
    public void setDropDirection(float pitch, float yaw) {
        this.interactic$dropPitch = pitch;
        this.interactic$dropYaw = yaw;
    }

    @Override
    public float getDropPitch() {
        return interactic$dropPitch;
    }

    @Override
    public float getDropYaw() {
        return interactic$dropYaw;
    }
}
