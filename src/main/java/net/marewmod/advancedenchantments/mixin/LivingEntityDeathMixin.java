package net.marewmod.advancedenchantments.mixin;

import net.marewmod.advancedenchantments.AdvancedEnchantmentsMod;
import net.marewmod.advancedenchantments.AdvancedEnchantmentsConfig;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityDeathMixin {

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void magnetic_setFlag(DamageSource source, CallbackInfo ci) {
        if (!(source.getAttacker() instanceof ServerPlayerEntity player)) return;
        if (!AdvancedEnchantmentsConfig.isEnabled("magnetic")) return;
        if (EnchantmentHelper.getLevel(AdvancedEnchantmentsMod.MAGNETIC, player.getMainHandStack()) <= 0) return;
        AdvancedEnchantmentsMod.MAGNETIC_PLAYER.set(player);
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void magnetic_clearFlag(DamageSource source, CallbackInfo ci) {
        AdvancedEnchantmentsMod.MAGNETIC_PLAYER.remove();
    }
}
