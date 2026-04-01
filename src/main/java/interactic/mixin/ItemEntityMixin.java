package interactic.mixin;

import interactic.InteracticInit;
import interactic.util.Helpers;
import interactic.util.InteracticItemExtensions;
import interactic.util.ItemDamageSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements InteracticItemExtensions {

    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    public int age;

    @Shadow
    @Nullable
    public abstract Entity getOwner();

    @Unique
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> INTERACTIC_ROTATION =
            net.minecraft.network.syncher.SynchedEntityData.defineId(ItemEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    @Unique
    private boolean interactic$wasThrown;

    @Unique
    private boolean interactic$wasFullPower;

    private ItemEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    public float getRotation() {
        return this.getEntityData().get(INTERACTIC_ROTATION);
    }

    @Override
    public void setRotation(float rotation) {
        this.getEntityData().set(INTERACTIC_ROTATION, rotation);
    }

    @Override
    public void markThrown() {
        this.interactic$wasThrown = true;
    }

    @Override
    public void markFullPower() {
        this.interactic$wasFullPower = true;
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void interactic$initData(net.minecraft.network.syncher.SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(INTERACTIC_ROTATION, -1f);
    }

    @Inject(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;getItem()Lnet/minecraft/world/item/ItemStack;", shift = At.Shift.AFTER), cancellable = true)
    private void interactic$controlPickup(Player player, CallbackInfo ci) {
        if (Helpers.canPlayerPickUpItem(player, (ItemEntity) (Object) this)) return;
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void interactic$dealThrowingDamage(CallbackInfo ci) {
        if (!InteracticInit.getConfig().itemsActAsProjectiles()) return;
        if (age < 2) return;

        var world = this.level();
        if (world.isClientSide()) return;

        if (this.onGround()) this.interactic$wasThrown = false;
        if (!this.interactic$wasThrown) return;

        final var hasDamageModifiers = this.getItem().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .modifiers().stream().anyMatch(entry -> entry.attribute().is(Attributes.ATTACK_DAMAGE));
        if (!(this.interactic$wasFullPower || hasDamageModifiers)) return;

        var damage = new MutableDouble(2d);
        if (hasDamageModifiers) {
            this.getItem().forEachModifier(EquipmentSlot.MAINHAND, (attribEntry, modifier) -> {
                if (!attribEntry.is(Attributes.ATTACK_DAMAGE) || modifier.operation() != AttributeModifier.Operation.ADD_VALUE) return;
                damage.add(modifier.amount());
            });
        }

        AABB box = this.getBoundingBox().inflate(0.15);
        List<? extends LivingEntity> entities = world.getEntities((Entity) null, box, e -> e instanceof LivingEntity le && le.isAlive() && !le.isSpectator())
                .stream()
                .map(e -> (LivingEntity) e)
                .toList();
        if (entities.isEmpty()) return;

        final var target = entities.getFirst();
        final var damageSource = new ItemDamageSource((ItemEntity) (Object) this, this.getOwner());

        if (target.hurtTime != 0 || target.isInvulnerableTo((ServerLevel) world, damageSource)) return;

        target.hurtServer((ServerLevel) world, damageSource, damage.floatValue());
        this.getItem().hurtAndBreak(1, (ServerLevel) world, null, item -> this.discard());
    }

    @Override
    public float getPickRadius() {
        return .2f;
    }
}
