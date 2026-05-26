package net.marewmod.advancedenchants;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.marewmod.advancedenchants.enchantment.DoubleShotEnchantment;
import net.marewmod.advancedenchants.enchantment.EvasionEnchantment;
import net.marewmod.advancedenchants.enchantment.HomingEnchantment;
import net.marewmod.advancedenchants.enchantment.ExperienceEnchantment;
import net.marewmod.advancedenchants.enchantment.FlameWalkerEnchantment;
import net.marewmod.advancedenchants.enchantment.LifestealEnchantment;
import net.marewmod.advancedenchants.enchantment.MagneticEnchantment;
import net.marewmod.advancedenchants.enchantment.ReplantEnchantment;
import net.marewmod.advancedenchants.enchantment.AutoSmeltEnchantment;
import net.marewmod.advancedenchants.enchantment.TreecapitatorEnchantment;
import net.marewmod.advancedenchants.enchantment.TripleShotEnchantment;
import net.marewmod.advancedenchants.enchantment.VeinMinerEnchantment;
import net.minecraft.block.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.item.ItemGroup;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetNbtLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class AdvancedEnchantsMod implements ModInitializer {

    public static final String MOD_ID = "advancedenchants";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Enchantment LIFESTEAL = Registry.register(
        Registries.ENCHANTMENT, new Identifier(MOD_ID, "lifesteal"), new LifestealEnchantment());

    public static final Enchantment REPLANT = Registry.register(
        Registries.ENCHANTMENT, new Identifier(MOD_ID, "replant"), new ReplantEnchantment());

    public static final Enchantment MAGNETIC = Registry.register(
        Registries.ENCHANTMENT, new Identifier(MOD_ID, "magnetic"), new MagneticEnchantment());

    public static final Enchantment FLAME_WALKER = Registry.register(
        Registries.ENCHANTMENT, new Identifier(MOD_ID, "flame_walker"), new FlameWalkerEnchantment());

    public static final Enchantment EXPERIENCE = Registry.register(
        Registries.ENCHANTMENT, new Identifier(MOD_ID, "experience"), new ExperienceEnchantment());

    public static final Enchantment DOUBLE_SHOT = Registry.register(
        Registries.ENCHANTMENT, new Identifier(MOD_ID, "double_shot"), new DoubleShotEnchantment());

    public static final Enchantment TRIPLE_SHOT = Registry.register(
        Registries.ENCHANTMENT, new Identifier(MOD_ID, "triple_shot"), new TripleShotEnchantment());

    public static final Enchantment TREECAPITATOR = Registry.register(
        Registries.ENCHANTMENT, new Identifier(MOD_ID, "treecapitator"), new TreecapitatorEnchantment());

    public static final Enchantment VEIN_MINER = Registry.register(
        Registries.ENCHANTMENT, new Identifier(MOD_ID, "vein_miner"), new VeinMinerEnchantment());

    public static final Enchantment AUTO_SMELT = Registry.register(
        Registries.ENCHANTMENT, new Identifier(MOD_ID, "auto_smelt"), new AutoSmeltEnchantment());

    public static final Enchantment HOMING = Registry.register(
        Registries.ENCHANTMENT, new Identifier(MOD_ID, "homing"), new HomingEnchantment());

    public static final Enchantment EVASION = Registry.register(
        Registries.ENCHANTMENT, new Identifier(MOD_ID, "evasion"), new EvasionEnchantment());

    public static final ThreadLocal<ServerPlayerEntity> MAGNETIC_PLAYER   = ThreadLocal.withInitial(() -> null);
    public static final ThreadLocal<ServerPlayerEntity> AUTO_SMELT_PLAYER = ThreadLocal.withInitial(() -> null);

    public static final Map<PersistentProjectileEntity, Integer> HOMING_ARROWS = new WeakHashMap<>();

    private static final ThreadLocal<Boolean> CHOPPING = ThreadLocal.withInitial(() -> false);

    private record ReplantTask(ServerWorld world, BlockPos pos, Block block, Item seedItem) {}
    private record TimedBlock(BlockPos pos, long expiryTick) {}
    private static final Queue<ReplantTask> REPLANT_QUEUE = new ArrayDeque<>();
    private static final Map<UUID, ArrayDeque<TimedBlock>> FLAME_WALKER_BLOCKS = new HashMap<>();

    @Override
    public void onInitialize() {
        AdvancedEnchantsConfig.load();

        // Reload config from disk before loot tables are rebuilt (i.e. on /reload)
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resourceManager) ->
            AdvancedEnchantsConfig.load());

        // After /reload, strip disabled enchantments from all online players immediately
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (!success) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                boolean stripped = false;
                for (int i = 0; i < player.getInventory().size(); i++) {
                    if (AdvancedEnchantsConfig.stripDisabled(player.getInventory().getStack(i))) stripped = true;
                }
                if (stripped) {
                    player.getInventory().markDirty();
                    player.sendMessage(Text.literal(
                        "[Advanced Enchants] One or more disabled enchantments were removed from your items."));
                }
            }
        });

        // ── Config-driven loot tables ─────────────────────────────────────────
        LootTableEvents.MODIFY.register((rm, manager, id, tableBuilder, source) -> {
            if (!source.isBuiltin()) return;
            String tableStr = id.toString();
            for (Map.Entry<String, AdvancedEnchantsConfig.EnchantmentConfig> entry
                    : AdvancedEnchantsConfig.get().enchantments.entrySet()) {
                if (!entry.getValue().enabled) continue;
                Enchantment ench = Registries.ENCHANTMENT.get(new Identifier(MOD_ID, entry.getKey()));
                if (ench == null) continue;
                for (AdvancedEnchantsConfig.LootEntry le : entry.getValue().loot) {
                    if (!AdvancedEnchantsConfig.matchesTable(le.table, tableStr)) continue;
                    ItemStack book = Items.ENCHANTED_BOOK.getDefaultStack();
                    EnchantedBookItem.addEnchantment(book, new EnchantmentLevelEntry(ench, le.level));
                    tableBuilder.pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .with(ItemEntry.builder(Items.ENCHANTED_BOOK)
                            .apply(SetNbtLootFunction.builder(book.getNbt())))
                        .conditionally(RandomChanceLootCondition.builder(le.chance)));
                }
            }
        });

        // ── Config-driven trades ──────────────────────────────────────────────
        for (Map.Entry<String, AdvancedEnchantsConfig.EnchantmentConfig> enchEntry
                : AdvancedEnchantsConfig.get().enchantments.entrySet()) {
            if (!enchEntry.getValue().enabled) continue;
            String enchId = enchEntry.getKey();
            for (AdvancedEnchantsConfig.TradeEntry te : enchEntry.getValue().trades) {
                if ("villager".equals(te.type)) {
                    VillagerProfession prof = Registries.VILLAGER_PROFESSION
                        .get(Identifier.tryParse(te.profession));
                    if (prof == null) {
                        LOGGER.warn("[AdvancedEnchants] Unknown profession '{}' for '{}' trade – skipping",
                            te.profession, enchId);
                        continue;
                    }
                    int capturedVillagerLevel = te.villager_level;
                    int capturedEnchLevel     = te.ench_level;
                    int capturedCost          = te.cost;
                    int capturedMaxUses       = te.max_uses;
                    TradeOfferHelper.registerVillagerOffers(prof, capturedVillagerLevel, factories ->
                        factories.add((entity, random) -> {
                            if (!AdvancedEnchantsConfig.isEnabled(enchId)) return null;
                            if (!AdvancedEnchantsConfig.isTradesEnabled(enchId)) return null;
                            Enchantment ench = Registries.ENCHANTMENT.get(new Identifier(MOD_ID, enchId));
                            if (ench == null) return null;
                            ItemStack book = Items.ENCHANTED_BOOK.getDefaultStack();
                            EnchantedBookItem.addEnchantment(book, new EnchantmentLevelEntry(ench, capturedEnchLevel));
                            return new TradeOffer(new ItemStack(Items.EMERALD, capturedCost),
                                book, capturedMaxUses, 10, 0.05f);
                        }));
                } else if ("wandering_trader".equals(te.type)) {
                    int capturedTier      = te.tier;
                    int capturedEnchLevel = te.ench_level;
                    int capturedCost      = te.cost;
                    int capturedMaxUses   = te.max_uses;
                    TradeOfferHelper.registerWanderingTraderOffers(capturedTier, factories ->
                        factories.add((entity, random) -> {
                            if (!AdvancedEnchantsConfig.isEnabled(enchId)) return null;
                            if (!AdvancedEnchantsConfig.isTradesEnabled(enchId)) return null;
                            Enchantment ench = Registries.ENCHANTMENT.get(new Identifier(MOD_ID, enchId));
                            if (ench == null) return null;
                            ItemStack book = Items.ENCHANTED_BOOK.getDefaultStack();
                            EnchantedBookItem.addEnchantment(book, new EnchantmentLevelEntry(ench, capturedEnchLevel));
                            return new TradeOffer(new ItemStack(Items.EMERALD, capturedCost),
                                book, capturedMaxUses, 5, 0.05f);
                        }));
                }
            }
        }

        // ── Strip disabled enchantments on player join ────────────────────────
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            boolean stripped = false;
            for (int i = 0; i < player.getInventory().size(); i++) {
                if (AdvancedEnchantsConfig.stripDisabled(player.getInventory().getStack(i))) stripped = true;
            }
            if (stripped) {
                player.getInventory().markDirty();
                player.sendMessage(Text.literal(
                    "[Advanced Enchants] One or more disabled enchantments were removed from your items."));
            }
        });

        // ── Flame Walker solidify lava + Magnetic XP absorption (every tick) ─
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!(player.getWorld() instanceof ServerWorld sw)) continue;

                // Always expire obsidian whether or not the enchantment is currently enabled
                ArrayDeque<TimedBlock> placed = FLAME_WALKER_BLOCKS.get(player.getUuid());
                if (placed != null && !placed.isEmpty()) {
                    long now = sw.getTime();
                    placed.removeIf(tb -> {
                        if (now >= tb.expiryTick()) {
                            if (sw.getBlockState(tb.pos()).isOf(Blocks.OBSIDIAN))
                                sw.setBlockState(tb.pos(), Blocks.LAVA.getDefaultState());
                            return true;
                        }
                        return false;
                    });
                }

                if (AdvancedEnchantsConfig.isEnabled("magnetic")
                        && EnchantmentHelper.getLevel(MAGNETIC, player.getMainHandStack()) > 0) {
                    sw.getEntitiesByClass(ExperienceOrbEntity.class,
                            player.getBoundingBox().expand(8.0), e -> !e.isRemoved())
                        .forEach(orb -> { player.addExperience(orb.getExperienceAmount()); orb.discard(); });
                }

                if (!player.isOnGround()) continue;
                if (!AdvancedEnchantsConfig.isEnabled("flame_walker")) continue;
                int level = EnchantmentHelper.getLevel(FLAME_WALKER, player.getEquippedStack(EquipmentSlot.FEET));
                if (level <= 0) continue;
                solidifyLava(sw, player, player.getBlockPos(), level);
            }
        });

        // ── Replant queue: populate on break, drain next tick ─────────────────
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld sw)) return;
            if (!world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) return;
            if (!AdvancedEnchantsConfig.isEnabled("replant")) return;
            if (EnchantmentHelper.getLevel(REPLANT, player.getMainHandStack()) <= 0) return;
            Block block = state.getBlock();
            if (!isCrop(block)) return;
            Item seedItem = getSeedForCrop(block);
            if (seedItem == null) return;
            REPLANT_QUEUE.add(new ReplantTask(sw, pos.toImmutable(), block, seedItem));
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            while (!REPLANT_QUEUE.isEmpty()) {
                final ReplantTask task = REPLANT_QUEUE.poll();
                if (task == null) break;
                Box box = new Box(task.pos()).expand(1.5);
                List<ItemEntity> dropped = task.world().getEntitiesByClass(ItemEntity.class, box,
                    e -> !e.isRemoved() && e.getStack().getItem() == task.seedItem());
                if (dropped.isEmpty()) continue;
                ItemStack stack = dropped.get(0).getStack();
                if (stack.getCount() > 1) stack.decrement(1);
                else dropped.get(0).discard();
                task.world().setBlockState(task.pos(), task.block().getDefaultState());
            }
        });

        // ── Treecapitator ─────────────────────────────────────────────────────
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld sw)) return;
            if (CHOPPING.get()) return;
            if (!AdvancedEnchantsConfig.isEnabled("treecapitator")) return;
            if (EnchantmentHelper.getLevel(TREECAPITATOR, player.getMainHandStack()) <= 0) return;
            if (!state.isIn(BlockTags.LOGS)) return;

            Block targetLog = state.getBlock();
            Set<BlockPos> logs = new HashSet<>();
            boolean isNatural = false;
            ArrayDeque<BlockPos> bfsQueue = new ArrayDeque<>();
            bfsQueue.add(pos); logs.add(pos);

            bfs:
            while (!bfsQueue.isEmpty()) {
                BlockPos current = bfsQueue.poll();
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos neighbor = current.add(dx, dy, dz);
                    if (logs.contains(neighbor)) continue;
                    BlockState ns = sw.getBlockState(neighbor);
                    if (!isNatural && ns.isIn(BlockTags.LEAVES)
                            && ns.contains(LeavesBlock.PERSISTENT) && !ns.get(LeavesBlock.PERSISTENT))
                        isNatural = true;
                    if (ns.getBlock() == targetLog) {
                        logs.add(neighbor);
                        if (logs.size() >= 200) break bfs;
                        bfsQueue.add(neighbor);
                    }
                }
            }
            if (!isNatural) return;

            CHOPPING.set(true);
            try {
                ItemStack tool = player.getMainHandStack();
                for (BlockPos logPos : logs) {
                    if (logPos.equals(pos)) continue;
                    BlockState logState = sw.getBlockState(logPos);
                    if (logState.getBlock() != targetLog) continue;
                    Block.dropStacks(logState, sw, logPos, null, player, tool);
                    sw.setBlockState(logPos, Blocks.AIR.getDefaultState());
                    if (!player.getAbilities().creativeMode && tool.isDamageable()) {
                        tool.damage(1, player, p -> p.sendToolBreakStatus(Hand.MAIN_HAND));
                        if (tool.isEmpty()) break;
                    }
                }
            } finally { CHOPPING.set(false); }
        });

        // ── Vein Miner ────────────────────────────────────────────────────────
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld sw)) return;
            if (CHOPPING.get()) return;
            if (!AdvancedEnchantsConfig.isEnabled("vein_miner")) return;
            if (EnchantmentHelper.getLevel(VEIN_MINER, player.getMainHandStack()) <= 0) return;
            if (!state.isIn(ConventionalBlockTags.ORES)) return;

            Block targetOre = state.getBlock();
            Set<BlockPos> ores = new HashSet<>();
            ArrayDeque<BlockPos> bfsQueue = new ArrayDeque<>();
            bfsQueue.add(pos); ores.add(pos);
            while (!bfsQueue.isEmpty() && ores.size() < 64) {
                BlockPos current = bfsQueue.poll();
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.offset(dir);
                    if (ores.contains(neighbor)) continue;
                    if (sw.getBlockState(neighbor).getBlock() == targetOre) {
                        ores.add(neighbor); bfsQueue.add(neighbor);
                    }
                }
            }
            if (ores.size() <= 1) return;

            CHOPPING.set(true);
            try {
                ItemStack tool = player.getMainHandStack();
                for (BlockPos orePos : ores) {
                    if (orePos.equals(pos)) continue;
                    BlockState oreState = sw.getBlockState(orePos);
                    if (oreState.getBlock() != targetOre) continue;
                    Block.dropStacks(oreState, sw, orePos, null, player, tool);
                    sw.setBlockState(orePos, Blocks.AIR.getDefaultState());
                    if (!player.getAbilities().creativeMode && tool.isDamageable()) {
                        tool.damage(1, player, p -> p.sendToolBreakStatus(Hand.MAIN_HAND));
                        if (tool.isEmpty()) break;
                    }
                }
            } finally { CHOPPING.set(false); }
        });

        // ── Evasion ───────────────────────────────────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!AdvancedEnchantsConfig.isEnabled("evasion")) return true;
            int pieces = 0;
            for (EquipmentSlot slot : new EquipmentSlot[]{
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
                if (EnchantmentHelper.getLevel(EVASION, player.getEquippedStack(slot)) > 0) pieces++;
            if (pieces == 0) return true;
            return player.getRandom().nextFloat() >= pieces * 0.01f;
        });

        // ── Homing ────────────────────────────────────────────────────────────
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (HOMING_ARROWS.isEmpty()) return;
            if (!AdvancedEnchantsConfig.isEnabled("homing")) return;
            List<PersistentProjectileEntity> done = new ArrayList<>();
            for (Map.Entry<PersistentProjectileEntity, Integer> entry : new ArrayList<>(HOMING_ARROWS.entrySet())) {
                PersistentProjectileEntity arrow = entry.getKey();
                if (arrow.isRemoved()) { done.add(arrow); continue; }
                Vec3d arrowPos = arrow.getPos();

                Vec3d aimOrigin;
                Vec3d aimDir;
                if (arrow.getOwner() instanceof ServerPlayerEntity shooter) {
                    aimOrigin = shooter.getEyePos();
                    aimDir = shooter.getRotationVector();
                } else {
                    aimOrigin = arrowPos;
                    aimDir = arrow.getVelocity().normalize();
                }

                LivingEntity target = arrow.getWorld().getEntitiesByClass(
                    LivingEntity.class,
                    arrow.getBoundingBox().expand(24.0),
                    e -> e != arrow.getOwner() && e.isAlive() && !e.isSpectator()
                ).stream()
                    .filter(e -> {
                        Vec3d toE = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(aimOrigin).normalize();
                        return aimDir.dotProduct(toE) > 0.5;
                    })
                    .max(Comparator.comparingDouble(e -> {
                        Vec3d toE = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(aimOrigin).normalize();
                        return aimDir.dotProduct(toE);
                    }))
                    .orElse(null);
                if (target == null) continue;

                Vec3d aimAt = target.getPos().add(0, target.getHeight() * 0.5, 0);
                double dist = arrowPos.distanceTo(aimAt);
                if (dist > 5.0) continue;

                Vec3d current = arrow.getVelocity();
                double speed = current.length();
                if (speed < 0.01) continue;

                Vec3d toTarget = aimAt.subtract(arrowPos).normalize();
                Vec3d dir = current.normalize();
                if (toTarget.dotProduct(dir) < -0.5) continue;
                double turnFactor = dist <= 2.0 ? 0.5 : 0.3;
                Vec3d newDir = dir.add(toTarget.subtract(dir).multiply(turnFactor)).normalize();
                arrow.setVelocity(newDir.x * speed, newDir.y * speed, newDir.z * speed);
            }
            done.forEach(HOMING_ARROWS::remove);
        });

        // ── Creative tab ──────────────────────────────────────────────────────
        Registry.register(Registries.ITEM_GROUP, new Identifier(MOD_ID, "enchantment_books"),
            FabricItemGroup.builder()
                .displayName(Text.literal("Advanced Enchants"))
                .icon(() -> {
                    ItemStack icon = Items.ENCHANTED_BOOK.getDefaultStack();
                    EnchantedBookItem.addEnchantment(icon, new EnchantmentLevelEntry(HOMING, 1));
                    return icon;
                })
                .entries((context, entries) -> {
                    for (Enchantment ench : List.of(
                            LIFESTEAL, REPLANT, MAGNETIC, FLAME_WALKER, EXPERIENCE,
                            TREECAPITATOR, VEIN_MINER, AUTO_SMELT,
                            DOUBLE_SHOT, TRIPLE_SHOT, HOMING, EVASION)) {
                        for (int lvl = 1; lvl <= ench.getMaxLevel(); lvl++) {
                            ItemStack book = Items.ENCHANTED_BOOK.getDefaultStack();
                            EnchantedBookItem.addEnchantment(book, new EnchantmentLevelEntry(ench, lvl));
                            entries.add(book);
                        }
                    }
                })
                .build()
        );

        LOGGER.info("Advanced Enchants loaded.");
    }

    private static void solidifyLava(ServerWorld world, ServerPlayerEntity player, BlockPos playerPos, int level) {
        int radius = 2 + level;
        int px = playerPos.getX(), py = playerPos.getY(), pz = playerPos.getZ();
        long now = world.getTime();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        ArrayDeque<TimedBlock> placed = FLAME_WALKER_BLOCKS.computeIfAbsent(player.getUuid(), k -> new ArrayDeque<>());
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                mutable.set(px + dx, py - 1, pz + dz);
                if (world.getFluidState(mutable).isOf(Fluids.LAVA)
                        && world.getFluidState(mutable).isStill()
                        && world.getBlockState(mutable.up()).isAir()) {
                    BlockPos immutable = mutable.toImmutable();
                    world.setBlockState(immutable, Blocks.OBSIDIAN.getDefaultState());
                    placed.add(new TimedBlock(immutable, now + 100));
                }
            }
        }
    }

    private static boolean isCrop(Block block) {
        return block instanceof CropBlock || block instanceof NetherWartBlock;
    }

    private static Item getSeedForCrop(Block block) {
        if (block == Blocks.WHEAT)       return Items.WHEAT_SEEDS;
        if (block == Blocks.CARROTS)     return Items.CARROT;
        if (block == Blocks.POTATOES)    return Items.POTATO;
        if (block == Blocks.BEETROOTS)   return Items.BEETROOT_SEEDS;
        if (block == Blocks.NETHER_WART) return Items.NETHER_WART;
        return null;
    }
}
