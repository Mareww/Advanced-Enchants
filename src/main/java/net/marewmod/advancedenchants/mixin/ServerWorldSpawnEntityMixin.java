package net.marewmod.advancedenchants.mixin;

import net.marewmod.advancedenchants.AdvancedEnchantsMod;
import net.marewmod.advancedenchants.AdvancedEnchantsConfig;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public class ServerWorldSpawnEntityMixin {

    @Inject(method = "spawnEntity", at = @At("HEAD"))
    private void homing_markArrow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!AdvancedEnchantsConfig.isEnabled("homing")) return;
        if (!(entity instanceof PersistentProjectileEntity proj)) return;
        if (!(proj.getOwner() instanceof ServerPlayerEntity player)) return;
        int level = EnchantmentHelper.getLevel(AdvancedEnchantsMod.HOMING, player.getMainHandStack());
        if (level <= 0) level = EnchantmentHelper.getLevel(AdvancedEnchantsMod.HOMING, player.getOffHandStack());
        if (level <= 0) return;
        AdvancedEnchantsMod.HOMING_ARROWS.put(proj, level);
    }

    @Inject(method = "spawnEntity", at = @At("HEAD"))
    private void auto_smelt_interceptItem(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof ItemEntity itemEntity)) return;
        ServerPlayerEntity player = AdvancedEnchantsMod.AUTO_SMELT_PLAYER.get();
        if (player == null) return;

        ServerWorld world = (ServerWorld) (Object) this;
        if (world.getRandom().nextFloat() >= 0.8f) return;

        ItemStack drop = itemEntity.getStack();
        var recipe = world.getServer().getRecipeManager()
                .getFirstMatch(RecipeType.SMELTING, new SimpleInventory(drop), world);
        if (recipe.isEmpty()) return;
        ItemStack output = recipe.get().getOutput(world.getRegistryManager()).copy();
        output.setCount(Math.min(drop.getCount(), output.getMaxCount()));
        itemEntity.setStack(output);
    }

    @Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
    private void magnetic_interceptItem(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof ItemEntity itemEntity)) return;
        ServerPlayerEntity player = AdvancedEnchantsMod.MAGNETIC_PLAYER.get();
        if (player == null) return;

        ItemStack remaining = itemEntity.getStack().copy();
        player.getInventory().insertStack(remaining);
        player.getInventory().markDirty();

        if (remaining.isEmpty()) {
            cir.setReturnValue(false);
        } else {
            itemEntity.setStack(remaining);
        }
    }
}
