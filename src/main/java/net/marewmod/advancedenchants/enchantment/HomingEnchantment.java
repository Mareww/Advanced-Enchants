package net.marewmod.advancedenchants.enchantment;

import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;

public class HomingEnchantment extends BaseAdvancedEnchantment {

    public HomingEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.BOW, new EquipmentSlot[]{ EquipmentSlot.MAINHAND }, "homing");
    }

    @Override public int getMaxLevel() { return 1; }
    @Override public int getMinPower(int level) { return 10 + (level - 1) * 10; }
    @Override public int getMaxPower(int level) { return getMinPower(level) + 20; }
}
