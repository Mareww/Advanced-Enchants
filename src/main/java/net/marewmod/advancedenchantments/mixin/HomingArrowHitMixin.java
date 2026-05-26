package net.marewmod.advancedenchantments.mixin;

import net.marewmod.advancedenchantments.AdvancedEnchantmentsMod;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public class HomingArrowHitMixin {

    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void homingResetIframes(EntityHitResult hit, CallbackInfo ci) {
        PersistentProjectileEntity self = (PersistentProjectileEntity)(Object)this;
        // Check owner has Homing (avoids WeakHashMap GC edge cases)
        if (!(self.getOwner() instanceof PlayerEntity player)) return;
        int level = EnchantmentHelper.getLevel(AdvancedEnchantmentsMod.HOMING, player.getMainHandStack());
        if (level <= 0) return;
        if (!(hit.getEntity() instanceof LivingEntity living)) return;
        // Reset both the animation timer and the actual iframe timer + last damage
        living.hurtTime = 0;
        ((LivingEntityAccessor) living).setLastDamageTaken(0f);
    }
}
