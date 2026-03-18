package interactic.mixin;

import interactic.InteracticInit;
import interactic.util.InteracticItemExtensions;
import interactic.util.ItemEntityRenderStateAccessor;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {

    private static final float TWO_PI = (float) (Math.PI * 2);
    private static final float HALF_PI = (float) (Math.PI * 0.5);
    private static final float THREE_HALF_PI = (float) (Math.PI * 1.5);

    @Unique
    private final Random interactic$random = Random.create();

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void interactic$renderFancy(ItemEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
        if (!InteracticInit.getConfig().fancyItemRendering()) return;
        if (state.itemRenderState.isEmpty()) {
            ci.cancel();
            return;
        }

        Box bbox = state.itemRenderState.getModelBoundingBox();
        float modelDepth = (float) bbox.getLengthZ();
        var stateExt = (ItemEntityRenderStateAccessor) state;
        boolean treatAsDepthModel = stateExt.interactic$isBlockItem() && modelDepth > 0.0625f;
        float blockHeight = Math.min(1f, (float) bbox.getLengthY());
        boolean isFlatBlock = treatAsDepthModel && blockHeight <= 0.75f;
        float distanceToCenter = (float) ((0.5 - blockHeight + blockHeight / 2f) * 0.25);

        int seed = state.seed;
        this.interactic$random.setSeed(seed);

        matrices.push();

        matrices.translate(0, 0.125f, 0);
        if (treatAsDepthModel) matrices.translate(0, distanceToCenter, 0);

        float groundDistance = treatAsDepthModel ? distanceToCenter : (0.125f - 0.0625f);
        if (!treatAsDepthModel) groundDistance -= (state.renderedAmount - 1) * 0.05f;
        matrices.translate(0, -groundDistance, 0);

        matrices.translate(0, (this.interactic$random.nextDouble() - 0.5) * 0.005, 0);
        if (treatAsDepthModel && !isFlatBlock) matrices.translate(0, -.1, 0);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(stateExt.interactic$getYaw()));
        float angle = stateExt.interactic$getCustomRotation();

        // keep angle in [0, 2PI)
        while (angle < 0) angle += TWO_PI;
        while (angle >= TWO_PI) angle -= TWO_PI;

        if (stateExt.interactic$isOnGround()) {
            // choose nearest of 0 or PI
            float target = (angle > HALF_PI && angle < THREE_HALF_PI) ? (float) Math.PI : 0f;
            // quickly ease towards target
            angle = MathHelper.lerp(0.4f, angle, target);
            if (Math.abs(angle - target) < 0.02f) {
                angle = target;
            }
        }

        if (treatAsDepthModel) matrices.translate(0, -distanceToCenter, 0);
        if (stateExt.interactic$isOnGround()) {
            // On ground: force items flush with the ground
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(isFlatBlock ? 0f : HALF_PI));
        } else {
            // In air: apply full tumble
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(angle + (isFlatBlock ? 0 : HALF_PI)));
        }

        if (treatAsDepthModel && !isFlatBlock && !InteracticInit.getConfig().blocksLayFlat()) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.interactic$random.nextFloat() * 45));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.interactic$random.nextFloat() * 45));
        }

        if (treatAsDepthModel) {
            matrices.translate(0, distanceToCenter, 0);
        }

        ItemEntityRenderer.render(matrices, queue, state.light, state, this.interactic$random, bbox);

        matrices.pop();
        ci.cancel();
    }

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void setInteracticState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        var stateExt = (ItemEntityRenderStateAccessor) state;
        var rotator = (InteracticItemExtensions) entity;
        float angle = rotator.getRotation();

        if (angle == -1f) {
            Random r = Random.create(state.seed + entity.getId());
            angle = (r.nextInt(20) - 10) * 0.15f;
        }

        if (!entity.isOnGround()) {
            double vy = entity.getVelocity().y;
            float spin = (float) (MathHelper.clamp(vy * 0.25, 0.075, 0.3)
                    * (entity.isSubmergedInWater() ? 0.25f : 1f)
                    * (tickDelta * 5f)
                    * InteracticInit.getItemRotationSpeedMultiplier());
            angle += spin;
            if (angle >= TWO_PI) angle -= TWO_PI;
        }

        rotator.setRotation(angle);
        stateExt.interactic$setCustomRotation(angle);
        stateExt.interactic$setLayFlat(InteracticInit.getConfig().blocksLayFlat());
        stateExt.interactic$setOnGround(entity.isOnGround());
        stateExt.interactic$setYaw(entity.getYaw());
        stateExt.interactic$setTickDelta(tickDelta);
        stateExt.interactic$setBlockItem(entity.getStack().getItem() instanceof BlockItem);
    }
}
