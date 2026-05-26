package net.marewmod.advancedenchantments.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Vanishable;

public class MagneticEnchantment extends BaseAdvancedEnchantment {

    public MagneticEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.VANISHABLE, new EquipmentSlot[]{ EquipmentSlot.MAINHAND }, "magnetic");
    }

    @Override
    public int getMaxLevel() { return 1; }

    @Override
    public int getMinPower(int level) { return 15; }

    @Override
    public int getMaxPower(int level) { return 50; }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof Vanishable;
    }
}
