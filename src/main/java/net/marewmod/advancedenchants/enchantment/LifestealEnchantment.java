package net.marewmod.advancedenchants.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;

public class LifestealEnchantment extends BaseAdvancedEnchantment {

    // Heal percentages per level: 5%, 8%, 12%
    public static final float[] HEAL_PERCENT = { 0f, 0.05f, 0.08f, 0.12f };

    public LifestealEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentTarget.WEAPON, new EquipmentSlot[]{ EquipmentSlot.MAINHAND }, "lifesteal");
    }

    @Override
    public int getMaxLevel() { return 3; }

    @Override
    public int getMinPower(int level) { return 10 + (level - 1) * 12; }

    @Override
    public int getMaxPower(int level) { return getMinPower(level) + 25; }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof SwordItem;
    }
}
