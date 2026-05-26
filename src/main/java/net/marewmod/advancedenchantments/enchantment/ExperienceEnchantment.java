package net.marewmod.advancedenchantments.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;

public class ExperienceEnchantment extends BaseAdvancedEnchantment {

    public ExperienceEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentTarget.WEAPON,
              new EquipmentSlot[]{ EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND }, "experience");
    }

    @Override public int getMaxLevel() { return 3; }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof SwordItem
            || stack.getItem() instanceof AxeItem
            || stack.getItem() instanceof PickaxeItem;
    }
}
