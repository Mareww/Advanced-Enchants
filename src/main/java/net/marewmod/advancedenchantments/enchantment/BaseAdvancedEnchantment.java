package net.marewmod.advancedenchantments.enchantment;

import net.marewmod.advancedenchantments.AdvancedEnchantmentsConfig;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;

public abstract class BaseAdvancedEnchantment extends Enchantment {

    private final String enchId;

    protected BaseAdvancedEnchantment(Rarity rarity, EnchantmentTarget target,
                                       EquipmentSlot[] slots, String enchId) {
        super(rarity, target, slots);
        this.enchId = enchId;
    }

    @Override
    public boolean isTreasure() {
        return !AdvancedEnchantmentsConfig.isTableEnchantable(enchId);
    }

    @Override
    public Rarity getRarity() {
        int w = AdvancedEnchantmentsConfig.getConfiguredWeight(enchId);
        if (w <= 0)  return super.getRarity();
        if (w >= 10) return Rarity.COMMON;
        if (w >= 5)  return Rarity.UNCOMMON;
        if (w >= 2)  return Rarity.RARE;
        return Rarity.VERY_RARE;
    }
}
