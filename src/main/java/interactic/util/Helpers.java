package interactic.util;

import interactic.InteracticInit;
import interactic.ItemFilterItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class Helpers {

    public static ItemEntity raycastItem(Entity camera, float reach) {
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(camera, e -> e instanceof ItemEntity, reach);
        if (hit instanceof EntityHitResult er && er.getEntity() instanceof ItemEntity item) {
            Vec3 from = new Vec3(camera.getX(), camera.getY(), camera.getZ());
            double dist = from.distanceTo(hit.getLocation()) - .3;
            if (camera.pick(dist, 1f, false) instanceof BlockHitResult blockResult) {
                if (!camera.level().getBlockState(blockResult.getBlockPos()).getCollisionShape(camera.level(), blockResult.getBlockPos()).isEmpty()) {
                    return null;
                }
            }
            return item;
        }
        return null;
    }

    public static boolean canPlayerPickUpItem(Player player, ItemEntity item) {
        if (!InteracticInit.getConfig().autoPickup() && player.isShiftKeyDown()) {
            return true;
        }

        if (!InteracticInit.getConfig().itemFilterEnabled()) return true;
        var allStacks = new ArrayList<ItemStack>();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            allStacks.add(inv.getItem(i));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.BODY && slot != EquipmentSlot.SADDLE)
                allStacks.add(player.getItemBySlot(slot));
        }
        var filters = allStacks.stream()
                .filter(stack -> stack.is(InteracticInit.getItemFilter()))
                .filter(stack -> stack.getOrDefault(ItemFilterItem.ENABLED, false))
                .map(stack -> new FilterEntry(stack, ItemFilterItem.getItemsInFilter(stack), stack.getOrDefault(ItemFilterItem.BLOCK_MODE, false)))
                .toList();

        if (filters.isEmpty()) return true;

        var allowed = filters.stream().allMatch(FilterEntry::blockMode);
        for (var entry : filters) {
            if (entry.blockMode) continue;

            if (entry.filterItems.contains(item.getItem().getItem())) {
                return true;
            }
        }

        if (!allowed) return false;

        for (var entry : filters) {
            if (!entry.blockMode) continue;

            if (entry.filterItems.contains(item.getItem().getItem())) {
                return false;
            }
        }

        return true;
    }

    private record FilterEntry(ItemStack filter, List<Item> filterItems, boolean blockMode) {}
}
