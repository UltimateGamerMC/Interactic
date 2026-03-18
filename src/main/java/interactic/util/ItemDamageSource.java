package interactic.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class ItemDamageSource extends DamageSource {

    public ItemDamageSource(ItemEntity projectile, @Nullable Entity attacker) {
        super(projectile.getEntityWorld().getDamageSources().thrown(projectile, attacker).getTypeRegistryEntry(), projectile, attacker);
    }

    @Override
    public Text getDeathMessage(LivingEntity entity) {
        Text attackerName = this.getAttacker() == null ? this.getSource().getDisplayName() : this.getAttacker().getDisplayName();
        ItemStack itemStack = ((ItemEntity) this.getSource()).getStack();
        String key = "death.attack.thrown_item";
        if (itemStack.contains(DataComponentTypes.WEAPON)) {
            String path = net.minecraft.registry.Registries.ITEM.getId(itemStack.getItem()).getPath();
            if (path.contains("sword")) key = key + ".sword";
            else if (path.contains("axe")) key = key + ".axe";
            else if (path.contains("pickaxe")) key = key + ".pickaxe";
            else if (path.contains("shovel")) key = key + ".shovel";
            else if (path.contains("hoe")) key = key + ".hoe";
        }
        return Text.translatable(key, entity.getDisplayName(), attackerName, itemStack.toHoverableText());
    }
}
