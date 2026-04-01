package interactic.mixin;

import interactic.util.ItemEntityRenderStateAccessor;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements ItemEntityRenderStateAccessor {

    @Unique
    public float interactic$customRotation = -1f;

    @Unique
    public boolean interactic$layFlat;

    @Unique
    public float interactic$yaw;

    @Unique
    public boolean interactic$onGround;

    @Unique
    public float interactic$tickDelta;

    @Unique
    public boolean interactic$isBlockItem;

    @Override
    public float interactic$getCustomRotation() {
        return interactic$customRotation;
    }

    @Override
    public void interactic$setCustomRotation(float value) {
        this.interactic$customRotation = value;
    }

    @Override
    public boolean interactic$isLayFlat() {
        return interactic$layFlat;
    }

    @Override
    public void interactic$setLayFlat(boolean value) {
        this.interactic$layFlat = value;
    }

    @Override
    public float interactic$getYaw() {
        return interactic$yaw;
    }

    @Override
    public void interactic$setYaw(float value) {
        this.interactic$yaw = value;
    }

    @Override
    public boolean interactic$isOnGround() {
        return interactic$onGround;
    }

    @Override
    public void interactic$setOnGround(boolean value) {
        this.interactic$onGround = value;
    }

    @Override
    public float interactic$getTickDelta() {
        return interactic$tickDelta;
    }

    @Override
    public void interactic$setTickDelta(float value) {
        this.interactic$tickDelta = value;
    }

    @Override
    public boolean interactic$isBlockItem() {
        return interactic$isBlockItem;
    }

    @Override
    public void interactic$setBlockItem(boolean value) {
        this.interactic$isBlockItem = value;
    }
}
