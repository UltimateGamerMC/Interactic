package interactic.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemDamageSource extends DamageSource {

    public ItemDamageSource(ItemEntity projectile, @Nullable Entity attacker) {
        super(projectile.level().damageSources().thrown(projectile, attacker).typeHolder(), projectile, attacker);
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity entity) {
        Component attackerName = this.getEntity() == null ? this.getDirectEntity().getDisplayName() : this.getEntity().getDisplayName();
        ItemStack itemStack = ((ItemEntity) this.getDirectEntity()).getItem();
        String key = "death.attack.thrown_item";
        if (itemStack.has(DataComponents.WEAPON)) {
            String path = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).getPath();
            if (path.contains("sword")) key = key + ".sword";
            else if (path.contains("axe")) key = key + ".axe";
            else if (path.contains("pickaxe")) key = key + ".pickaxe";
            else if (path.contains("shovel")) key = key + ".shovel";
            else if (path.contains("hoe")) key = key + ".hoe";
        }
        return Component.translatable(key, entity.getDisplayName(), attackerName, itemStack.getHoverName());
    }
}
