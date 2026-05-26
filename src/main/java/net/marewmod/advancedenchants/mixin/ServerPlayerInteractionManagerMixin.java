package net.marewmod.advancedenchants.mixin;

import net.marewmod.advancedenchants.AdvancedEnchantsMod;
import net.marewmod.advancedenchants.AdvancedEnchantsConfig;
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
        if (AdvancedEnchantsConfig.isEnabled("magnetic")
                && EnchantmentHelper.getLevel(AdvancedEnchantsMod.MAGNETIC, player.getMainHandStack()) > 0) {
            AdvancedEnchantsMod.MAGNETIC_PLAYER.set(player);
        }
        if (AdvancedEnchantsConfig.isEnabled("auto_smelt")
                && EnchantmentHelper.getLevel(AdvancedEnchantsMod.AUTO_SMELT, player.getMainHandStack()) > 0) {
            AdvancedEnchantsMod.AUTO_SMELT_PLAYER.set(player);
        }
    }

    @Inject(method = "tryBreakBlock", at = @At("RETURN"))
    private void clearFlags(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        AdvancedEnchantsMod.MAGNETIC_PLAYER.remove();
        AdvancedEnchantsMod.AUTO_SMELT_PLAYER.remove();
    }
}
