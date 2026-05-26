package net.marewmod.advancedenchantments.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;

public class VeinMinerEnchantment extends BaseAdvancedEnchantment {

    public VeinMinerEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.DIGGER,
              new EquipmentSlot[]{ EquipmentSlot.MAINHAND }, "vein_miner");
    }

    @Override public int getMaxLevel() { return 1; }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof PickaxeItem;
    }
}
