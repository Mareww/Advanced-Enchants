package net.marewmod.advancedenchantments.mixin;

import net.marewmod.advancedenchantments.AdvancedEnchantmentsMod;
import net.marewmod.advancedenchantments.AdvancedEnchantmentsConfig;
import net.marewmod.advancedenchantments.enchantment.LifestealEnchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerAttackMixin {

    @Unique
    private float lifesteal$charge = 0f;

    @Inject(method = "attack", at = @At("HEAD"))
    private void lifesteal_captureCharge(Entity target, CallbackInfo ci) {
        lifesteal$charge = ((PlayerEntity)(Object)this).getAttackCooldownProgress(0.5f);
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void lifesteal_onAttack(Entity target, CallbackInfo ci) {
        if (!(target instanceof LivingEntity living)) return;

        PlayerEntity player = (PlayerEntity)(Object)this;
        if (!(player instanceof ServerPlayerEntity)) return;
        if (lifesteal$charge < 0.9f) return;

        if (!AdvancedEnchantmentsConfig.isEnabled("lifesteal")) return;
        ItemStack weapon = player.getMainHandStack();
        int level = EnchantmentHelper.getLevel(AdvancedEnchantmentsMod.LIFESTEAL, weapon);
        if (level <= 0) return;

        float damage = (float) player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        damage += EnchantmentHelper.getAttackDamage(weapon, living.getGroup());

        player.heal(damage * LifestealEnchantment.HEAL_PERCENT[level]);
    }
}
