package net.marewmod.advancedenchants.enchantment;

import net.marewmod.advancedenchants.AdvancedEnchantsMod;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;

public class DoubleShotEnchantment extends BaseAdvancedEnchantment {

    public DoubleShotEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.BOW, new EquipmentSlot[]{ EquipmentSlot.MAINHAND }, "double_shot");
    }

    @Override public int getMaxLevel() { return 1; }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof BowItem;
    }

    @Override
    protected boolean canAccept(Enchantment other) {
        return super.canAccept(other)
            && other != AdvancedEnchantsMod.TRIPLE_SHOT
            && other != Enchantments.MULTISHOT;
    }
}
