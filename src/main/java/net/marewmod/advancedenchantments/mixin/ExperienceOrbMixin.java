package net.marewmod.advancedenchantments.mixin;

import net.marewmod.advancedenchantments.AdvancedEnchantmentsMod;
import net.marewmod.advancedenchantments.AdvancedEnchantmentsConfig;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbEntity.class)
public class ExperienceOrbMixin {

    @Shadow
    private int amount;

    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void experience_boost(PlayerEntity player, CallbackInfo ci) {
        if (player.getWorld().isClient()) return;
        if (!AdvancedEnchantmentsConfig.isEnabled("experience")) return;
        int level = Math.max(
            EnchantmentHelper.getLevel(AdvancedEnchantmentsMod.EXPERIENCE, player.getMainHandStack()),
            EnchantmentHelper.getLevel(AdvancedEnchantmentsMod.EXPERIENCE, player.getOffHandStack())
        );
        if (level <= 0) return;
        this.amount = Math.round(this.amount * (1.0f + level * 0.1f));
    }
}
