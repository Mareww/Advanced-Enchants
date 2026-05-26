package net.marewmod.advancedenchantments.mixin;

import net.marewmod.advancedenchantments.AdvancedEnchantmentsMod;
import net.marewmod.advancedenchantments.AdvancedEnchantmentsConfig;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {

    @Shadow protected ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock", at = @At("HEAD"))
    private void setFlags(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (AdvancedEnchantmentsConfig.isEnabled("magnetic")
                && EnchantmentHelper.getLevel(AdvancedEnchantmentsMod.MAGNETIC, player.getMainHandStack()) > 0) {
            AdvancedEnchantmentsMod.MAGNETIC_PLAYER.set(player);
        }
        if (AdvancedEnchantmentsConfig.isEnabled("auto_smelt")
                && EnchantmentHelper.getLevel(AdvancedEnchantmentsMod.AUTO_SMELT, player.getMainHandStack()) > 0) {
            AdvancedEnchantmentsMod.AUTO_SMELT_PLAYER.set(player);
        }
    }

    @Inject(method = "tryBreakBlock", at = @At("RETURN"))
    private void clearFlags(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        AdvancedEnchantmentsMod.MAGNETIC_PLAYER.remove();
        AdvancedEnchantmentsMod.AUTO_SMELT_PLAYER.remove();
    }
}
