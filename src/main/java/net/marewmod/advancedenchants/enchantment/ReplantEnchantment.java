package net.marewmod.advancedenchants.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;

public class ReplantEnchantment extends BaseAdvancedEnchantment {

    public ReplantEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentTarget.BREAKABLE, new EquipmentSlot[]{ EquipmentSlot.MAINHAND }, "replant");
    }

    @Override
    public int getMaxLevel() { return 1; }

    @Override
    public int getMinPower(int level) { return 15; }

    @Override
    public int getMaxPower(int level) { return 50; }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof HoeItem;
    }
}
