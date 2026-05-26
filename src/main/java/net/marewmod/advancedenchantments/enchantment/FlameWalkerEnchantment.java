package net.marewmod.advancedenchantments.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class FlameWalkerEnchantment extends BaseAdvancedEnchantment {

    public FlameWalkerEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.ARMOR_FEET, new EquipmentSlot[]{ EquipmentSlot.FEET }, "flame_walker");
    }

    @Override
    public int getMaxLevel() { return 2; }

    @Override
    public int getMinPower(int level) { return 10; }

    @Override
    public int getMaxPower(int level) { return 25; }

    @Override
    protected boolean canAccept(Enchantment other) {
        return super.canAccept(other) && other != Enchantments.FROST_WALKER;
    }
}
