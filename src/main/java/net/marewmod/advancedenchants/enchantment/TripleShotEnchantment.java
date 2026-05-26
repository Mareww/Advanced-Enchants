package net.marewmod.advancedenchants.enchantment;

import net.marewmod.advancedenchants.AdvancedEnchantsMod;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;

public class TripleShotEnchantment extends BaseAdvancedEnchantment {

    public TripleShotEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentTarget.BOW, new EquipmentSlot[]{ EquipmentSlot.MAINHAND }, "triple_shot");
    }

    @Override public int getMaxLevel() { return 1; }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof BowItem;
    }

    @Override
    protected boolean canAccept(Enchantment other) {
        return super.canAccept(other)
            && other != AdvancedEnchantsMod.DOUBLE_SHOT
            && other != Enchantments.MULTISHOT;
    }
}
