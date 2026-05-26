package net.marewmod.advancedenchants.enchantment;

import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;

public class AutoSmeltEnchantment extends BaseAdvancedEnchantment {

    public AutoSmeltEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentTarget.DIGGER,
              new EquipmentSlot[]{ EquipmentSlot.MAINHAND }, "auto_smelt");
    }

    @Override public int getMaxLevel() { return 1; }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof PickaxeItem;
    }
}
