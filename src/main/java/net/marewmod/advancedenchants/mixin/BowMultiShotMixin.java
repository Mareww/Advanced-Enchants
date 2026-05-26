package net.marewmod.advancedenchants.mixin;

import net.marewmod.advancedenchants.AdvancedEnchantsMod;
import net.marewmod.advancedenchants.AdvancedEnchantsConfig;
import net.marewmod.advancedenchants.compat.QuiverCompat;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Priority 1100 > quiver's default 1000 so our TAIL runs before quiver clears the shot tracker
@Mixin(value = BowItem.class, priority = 1100)
public class BowMultiShotMixin {

    @Inject(method = "onStoppedUsing", at = @At("TAIL"))
    private void extraShot(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        if (world.isClient()) return;
        if (!(user instanceof PlayerEntity player)) return;

        float pull = BowItem.getPullProgress(stack.getMaxUseTime() - remainingUseTicks);
        if (pull < 0.1f) return;

        int doubleLevel = AdvancedEnchantsConfig.isEnabled("double_shot")
            ? EnchantmentHelper.getLevel(AdvancedEnchantsMod.DOUBLE_SHOT, stack) : 0;
        int tripleLevel = AdvancedEnchantsConfig.isEnabled("triple_shot")
            ? EnchantmentHelper.getLevel(AdvancedEnchantsMod.TRIPLE_SHOT, stack) : 0;
        float[] pitchOffsets = tripleLevel > 0 ? new float[]{-5f, 5f}
                             : doubleLevel > 0 ? new float[]{5f}
                             : null;
        if (pitchOffsets == null) return;

        int power = EnchantmentHelper.getLevel(Enchantments.POWER, stack);
        boolean flame = EnchantmentHelper.getLevel(Enchantments.FLAME, stack) > 0;
        boolean creative = player.getAbilities().creativeMode;
        boolean infinity = EnchantmentHelper.getLevel(Enchantments.INFINITY, stack) > 0;

        // Determine how many extra arrows we can fire
        boolean usingQuiver = false;
        int quiverAvailable = -1;
        ItemStack arrowStack = ItemStack.EMPTY;

        if (!creative && !infinity) {
            // getProjectileType populates QuiverShotTracker if a quiver is active
            arrowStack = player.getProjectileType(stack);
            quiverAvailable = QuiverCompat.getAvailableCount();
            usingQuiver = quiverAvailable >= 0;
        }

        int extraCount;
        if (creative || infinity) {
            extraCount = pitchOffsets.length;
        } else if (usingQuiver) {
            // quiver hasn't consumed yet (quiver's TAIL runs after ours); subtract 1 for vanilla's shot
            extraCount = Math.min(pitchOffsets.length, Math.max(0, quiverAvailable - 1));
        } else {
            extraCount = Math.min(pitchOffsets.length, arrowStack.getCount());
        }

        for (int i = 0; i < extraCount; i++) {
            if (!creative && !infinity && !usingQuiver) {
                arrowStack.decrement(1);
                if (arrowStack.isEmpty()) player.getInventory().removeOne(arrowStack);
            }
            ArrowEntity arrow = new ArrowEntity(world, player);
            arrow.setVelocity(player, player.getPitch() + pitchOffsets[i], player.getYaw(), 0f, pull * 3f, 1f);
            arrow.setCritical(pull == 1f);
            if (power > 0) arrow.setDamage(arrow.getDamage() + power * 0.5 + 0.5);
            if (flame) arrow.setOnFireFor(100);
            arrow.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
            world.spawnEntity(arrow);
        }

        // Consume extra arrows from quiver (quiver's TAIL will consume the vanilla 1 separately)
        if (!creative && !infinity && usingQuiver) {
            QuiverCompat.consumeExtra(player, extraCount);
        }
    }
}
