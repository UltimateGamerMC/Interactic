package interactic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import interactic.InteracticInit;
import interactic.util.InteracticItemExtensions;
import interactic.util.ItemEntityRenderStateAccessor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.AABB;
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
    private final RandomSource interactic$random = RandomSource.create();

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void interactic$renderFancy(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        if (!InteracticInit.getConfig().fancyItemRendering()) return;
        if (state.item.isEmpty()) {
            ci.cancel();
            return;
        }

        AABB bbox = state.item.getModelBoundingBox();
        float modelDepth = (float) bbox.getZsize();
        var stateExt = (ItemEntityRenderStateAccessor) state;
        boolean treatAsDepthModel = stateExt.interactic$isBlockItem() && modelDepth > 0.0625f;
        float blockHeight = Math.min(1f, (float) bbox.getYsize());
        boolean isFlatBlock = treatAsDepthModel && blockHeight <= 0.75f;
        float distanceToCenter = (float) ((0.5 - blockHeight + blockHeight / 2f) * 0.25);

        int seed = state.seed;
        this.interactic$random.setSeed(seed);

        poseStack.pushPose();

        poseStack.translate(0, 0.125f, 0);
        if (treatAsDepthModel) poseStack.translate(0, distanceToCenter, 0);

        float groundDistance = treatAsDepthModel ? distanceToCenter : (0.125f - 0.0625f);
        if (!treatAsDepthModel) groundDistance -= (state.count - 1) * 0.05f;
        poseStack.translate(0, -groundDistance, 0);

        poseStack.translate(0, (this.interactic$random.nextDouble() - 0.5) * 0.005, 0);
        if (treatAsDepthModel && !isFlatBlock) poseStack.translate(0, -.1, 0);

        poseStack.mulPose(Axis.YP.rotationDegrees(stateExt.interactic$getYaw()));
        float angle = stateExt.interactic$getCustomRotation();

        while (angle < 0) angle += TWO_PI;
        while (angle >= TWO_PI) angle -= TWO_PI;

        if (stateExt.interactic$isOnGround()) {
            float target = (angle > HALF_PI && angle < THREE_HALF_PI) ? (float) Math.PI : 0f;
            angle = Mth.lerp(0.4f, angle, target);
            if (Math.abs(angle - target) < 0.02f) {
                angle = target;
            }
        }

        if (treatAsDepthModel) poseStack.translate(0, -distanceToCenter, 0);
        if (stateExt.interactic$isOnGround()) {
            poseStack.mulPose(Axis.XP.rotation(isFlatBlock ? 0f : HALF_PI));
        } else {
            poseStack.mulPose(Axis.XP.rotation(angle + (isFlatBlock ? 0 : HALF_PI)));
        }

        if (treatAsDepthModel && !isFlatBlock && !InteracticInit.getConfig().blocksLayFlat()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(this.interactic$random.nextFloat() * 45));
            poseStack.mulPose(Axis.ZP.rotationDegrees(this.interactic$random.nextFloat() * 45));
        }

        if (treatAsDepthModel) {
            poseStack.translate(0, distanceToCenter, 0);
        }

        ItemEntityRenderer.submitMultipleFromCount(poseStack, submitNodeCollector, state.lightCoords, state, this.interactic$random, bbox);

        poseStack.popPose();
        ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void interactic$setState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        var stateExt = (ItemEntityRenderStateAccessor) state;
        var rotator = (InteracticItemExtensions) entity;
        float angle = rotator.getRotation();

        if (angle == -1f) {
            RandomSource r = RandomSource.create(state.seed + entity.getId());
            angle = (r.nextInt(20) - 10) * 0.15f;
        }

        if (!entity.onGround()) {
            double vy = entity.getDeltaMovement().y;
            float spin = (float) (Mth.clamp(vy * 0.25, 0.075, 0.3)
                    * (entity.isInWater() ? 0.25f : 1f)
                    * (tickDelta * 5f)
                    * InteracticInit.getItemRotationSpeedMultiplier());
            angle += spin;
            if (angle >= TWO_PI) angle -= TWO_PI;
        }

        rotator.setRotation(angle);
        stateExt.interactic$setCustomRotation(angle);
        stateExt.interactic$setLayFlat(InteracticInit.getConfig().blocksLayFlat());
        stateExt.interactic$setOnGround(entity.onGround());
        stateExt.interactic$setYaw(entity.getYRot());
        stateExt.interactic$setTickDelta(tickDelta);
        stateExt.interactic$setBlockItem(entity.getItem().getItem() instanceof BlockItem);
    }
}
