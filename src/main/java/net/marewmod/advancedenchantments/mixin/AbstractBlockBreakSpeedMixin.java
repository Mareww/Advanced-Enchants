package net.marewmod.advancedenchantments.mixin;

import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.marewmod.advancedenchantments.AdvancedEnchantmentsMod;
import net.marewmod.advancedenchantments.AdvancedEnchantmentsConfig;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(AbstractBlock.class)
public class AbstractBlockBreakSpeedMixin {

    private static final Map<UUID, BlockPos> LAST_POS  = new ConcurrentHashMap<>();
    private static final Map<UUID, int[]>    LAST_SCAN = new ConcurrentHashMap<>();

    @Inject(method = "calcBlockBreakingDelta", at = @At("RETURN"), cancellable = true)
    private void multiBreakSlowdown(BlockState state, PlayerEntity player, BlockView world, BlockPos pos,
                                    CallbackInfoReturnable<Float> cir) {
        ItemStack tool = player.getMainHandStack();
        boolean hasTreecap  = AdvancedEnchantmentsConfig.isEnabled("treecapitator")
            && state.isIn(BlockTags.LOGS) && EnchantmentHelper.getLevel(AdvancedEnchantmentsMod.TREECAPITATOR, tool) > 0;
        boolean hasVeinMiner = AdvancedEnchantmentsConfig.isEnabled("vein_miner")
            && state.isIn(ConventionalBlockTags.ORES) && EnchantmentHelper.getLevel(AdvancedEnchantmentsMod.VEIN_MINER, tool) > 0;
        if (!hasTreecap && !hasVeinMiner) return;

        UUID uuid = player.getUuid();
        int[] scan;
        if (pos.equals(LAST_POS.get(uuid))) {
            scan = LAST_SCAN.get(uuid);
            if (scan == null) scan = scan(world, pos, state.getBlock(), hasTreecap);
        } else {
            scan = scan(world, pos, state.getBlock(), hasTreecap);
            LAST_SCAN.put(uuid, scan);
            LAST_POS.put(uuid, pos);
        }

        if (hasTreecap && scan[1] == 0) return; // not a natural tree
        int count = scan[0];
        if (count <= 1) return;
        cir.setReturnValue(cir.getReturnValue() / count);
    }

    // Returns [blockCount, isNatural (1=yes, 0=no)]
    // Treecapitator: 26-neighbour diagonal BFS, checks for non-persistent leaves, cap 200
    // Vein Miner:    6-direction face BFS, no naturalness check, cap 64
    private static int[] scan(BlockView world, BlockPos start, Block target, boolean isTree) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        boolean isNatural = false;
        int cap = isTree ? 200 : 64;

        outer:
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (isTree) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            BlockPos nb = current.add(dx, dy, dz);
                            if (visited.contains(nb)) continue;
                            BlockState ns = world.getBlockState(nb);
                            if (!isNatural && ns.isIn(BlockTags.LEAVES)
                                    && ns.contains(LeavesBlock.PERSISTENT)
                                    && !ns.get(LeavesBlock.PERSISTENT)) {
                                isNatural = true;
                            }
                            if (ns.getBlock() == target) {
                                visited.add(nb);
                                if (visited.size() >= cap) break outer;
                                queue.add(nb);
                            }
                        }
                    }
                }
            } else {
                for (Direction dir : Direction.values()) {
                    BlockPos nb = current.offset(dir);
                    if (visited.contains(nb)) continue;
                    if (world.getBlockState(nb).getBlock() == target) {
                        visited.add(nb);
                        if (visited.size() >= cap) break outer;
                        queue.add(nb);
                    }
                }
            }
        }
        return new int[]{ visited.size(), (!isTree || isNatural) ? 1 : 0 };
    }
}
