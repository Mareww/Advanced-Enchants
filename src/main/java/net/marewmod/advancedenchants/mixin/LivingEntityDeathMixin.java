package net.marewmod.advancedenchants.mixin;

import net.marewmod.advancedenchants.AdvancedEnchantsMod;
import net.marewmod.advancedenchants.AdvancedEnchantsConfig;
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
        if (!AdvancedEnchantsConfig.isEnabled("magnetic")) return;
        if (EnchantmentHelper.getLevel(AdvancedEnchantsMod.MAGNETIC, player.getMainHandStack()) <= 0) return;
        AdvancedEnchantsMod.MAGNETIC_PLAYER.set(player);
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void magnetic_clearFlag(DamageSource source, CallbackInfo ci) {
        AdvancedEnchantsMod.MAGNETIC_PLAYER.remove();
    }
}
