package net.marewmod.advancedenchants;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class AdvancedEnchantsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("advancedenchants");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ── Data model ────────────────────────────────────────────────────────────

    public static class LootEntry {
        /** Loot table ID; supports one '*' wildcard, e.g. "minecraft:chests/village/*_house" */
        public String table = "";
        public int level = 1;
        public float chance = 0.10f;
    }

    public static class TradeEntry {
        /** "villager" or "wandering_trader" */
        public String type = "villager";
        /** Villager profession ID, e.g. "minecraft:fletcher" */
        public String profession = "minecraft:fletcher";
        /** Villager tier (1–5) */
        public int villager_level = 1;
        /** Wandering trader tier */
        public int tier = 1;
        public int ench_level = 1;
        public int cost = 16;
        public int max_uses = 2;
    }

    public static class EnchantmentConfig {
        public boolean enabled = true;
        public boolean table_enchantable = false;
        public int weight = 0; // 0 = use class default; 10=common, 5=uncommon, 2=rare, 1=very_rare
        public boolean trades_enabled = true;
        public List<LootEntry> loot = new ArrayList<>();
        public List<TradeEntry> trades = new ArrayList<>();
    }

    /** Ordered map so the file is stable across saves */
    public Map<String, EnchantmentConfig> enchantments = new LinkedHashMap<>();

    /** Bumped when new fields are added — triggers one-time migration on old configs. */
    public int _config_version = 0;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static AdvancedEnchantsConfig INSTANCE;

    public static AdvancedEnchantsConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static boolean isEnabled(String id) {
        EnchantmentConfig c = get().enchantments.get(id);
        return c == null || c.enabled;
    }

    public static boolean isTableEnchantable(String id) {
        EnchantmentConfig c = get().enchantments.get(id);
        return c != null && c.table_enchantable;
    }

    /** Returns the configured weight, or 0 if using class default. */
    public static int getConfiguredWeight(String id) {
        EnchantmentConfig c = get().enchantments.get(id);
        return c == null ? 0 : c.weight;
    }

    public static boolean isTradesEnabled(String id) {
        EnchantmentConfig c = get().enchantments.get(id);
        return c == null || c.trades_enabled;
    }

    public static List<LootEntry> getLoot(String id) {
        EnchantmentConfig c = get().enchantments.get(id);
        return c == null ? List.of() : c.loot;
    }

    public static List<TradeEntry> getTrades(String id) {
        EnchantmentConfig c = get().enchantments.get(id);
        return c == null ? List.of() : c.trades;
    }

    // ── I/O ──────────────────────────────────────────────────────────────────

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("advancedenchants.json");
    }

    public static void load() {
        Path path = configPath();
        AdvancedEnchantsConfig defaults = buildDefaults();

        if (!Files.exists(path)) {
            INSTANCE = defaults;
            save();
            LOGGER.info("[AdvancedEnchants] Config created at {}", path);
            return;
        }

        try (Reader r = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            AdvancedEnchantsConfig loaded = GSON.fromJson(r, AdvancedEnchantsConfig.class);
            if (loaded == null) loaded = new AdvancedEnchantsConfig();
            // Merge: keep user data, add any newly introduced enchantments
            for (Map.Entry<String, EnchantmentConfig> entry : defaults.enchantments.entrySet()) {
                loaded.enchantments.putIfAbsent(entry.getKey(), entry.getValue());
            }
            // v2 migration: apply table_enchantable defaults for existing entries that predate this field
            if (loaded._config_version < 2) {
                for (Map.Entry<String, EnchantmentConfig> entry : defaults.enchantments.entrySet()) {
                    EnchantmentConfig ec = loaded.enchantments.get(entry.getKey());
                    if (ec != null) ec.table_enchantable = entry.getValue().table_enchantable;
                }
                loaded._config_version = 2;
            }
            // v3 migration: trades_enabled defaults to true for all existing entries
            if (loaded._config_version < 3) {
                for (EnchantmentConfig ec : loaded.enchantments.values())
                    ec.trades_enabled = true;
                loaded._config_version = 3;
            }
            // v4 migration: apply correct trades_enabled defaults (false for enchants with no trades)
            if (loaded._config_version < 4) {
                for (Map.Entry<String, EnchantmentConfig> entry : defaults.enchantments.entrySet()) {
                    EnchantmentConfig ec = loaded.enchantments.get(entry.getKey());
                    if (ec != null) ec.trades_enabled = entry.getValue().trades_enabled;
                }
                loaded._config_version = 4;
            }
            INSTANCE = loaded;
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("[AdvancedEnchants] Failed to read config – using defaults: {}", e.getMessage());
            INSTANCE = defaults;
        }

        for (Map.Entry<String, EnchantmentConfig> entry : INSTANCE.enchantments.entrySet()) {
            if (!entry.getValue().enabled) {
                LOGGER.warn("[AdvancedEnchants] '{}' is DISABLED. " +
                    "It will be stripped from player items on login.", entry.getKey());
            }
        }
        save(); // write back so newly added enchantments appear in the file
    }

    public static void save() {
        try {
            Files.writeString(configPath(), GSON.toJson(INSTANCE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("[AdvancedEnchants] Failed to save config: {}", e.getMessage());
        }
    }

    // ── Pattern matching ──────────────────────────────────────────────────────

    /** Matches a table ID against a pattern containing at most one '*' wildcard. */
    public static boolean matchesTable(String pattern, String tableId) {
        int star = pattern.indexOf('*');
        if (star < 0) return pattern.equals(tableId);
        String prefix = pattern.substring(0, star);
        String suffix = pattern.substring(star + 1);
        return tableId.startsWith(prefix)
            && tableId.endsWith(suffix)
            && tableId.length() >= prefix.length() + suffix.length();
    }

    // ── Strip disabled enchantments from a stack ──────────────────────────────

    /** Removes all disabled AE enchantments from a stack. Returns true if anything was removed. */
    public static boolean stripDisabled(ItemStack stack) {
        if (stack.isEmpty()) return false;
        boolean changed = false;

        // Regular enchantments ("Enchantments" tag)
        Map<Enchantment, Integer> enchMap = new LinkedHashMap<>(EnchantmentHelper.get(stack));
        Iterator<Map.Entry<Enchantment, Integer>> it = enchMap.entrySet().iterator();
        while (it.hasNext()) {
            Enchantment ench = it.next().getKey();
            net.minecraft.util.Identifier id = net.minecraft.registry.Registries.ENCHANTMENT.getId(ench);
            if (id != null && "advancedenchants".equals(id.getNamespace()) && !isEnabled(id.getPath())) {
                it.remove();
                changed = true;
            }
        }
        if (changed) EnchantmentHelper.set(enchMap, stack);

        // Stored enchantments on books ("StoredEnchantments" tag)
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("StoredEnchantments", 9)) {
            NbtList stored = nbt.getList("StoredEnchantments", 10);
            NbtList filtered = new NbtList();
            boolean removedAny = false;
            for (int i = 0; i < stored.size(); i++) {
                NbtCompound ec = stored.getCompound(i);
                String rawId = ec.getString("id");
                if (rawId.startsWith("advancedenchants:")) {
                    String path = rawId.substring("advancedenchants:".length());
                    if (!isEnabled(path)) { removedAny = true; continue; }
                }
                filtered.add(ec);
            }
            if (removedAny) {
                nbt.put("StoredEnchantments", filtered);
                changed = true;
            }
        }

        return changed;
    }

    // ── Default config ────────────────────────────────────────────────────────

    private static AdvancedEnchantsConfig buildDefaults() {
        AdvancedEnchantsConfig cfg = new AdvancedEnchantsConfig();
        cfg._config_version = 4;

        cfg.enchantments.put("lifesteal", enchCfg(
            new LootEntry[]{ loot("minecraft:chests/woodland_mansion", 1, 0.12f),
                             loot("minecraft:chests/woodland_mansion", 2, 0.10f),
                             loot("minecraft:chests/woodland_mansion", 3, 0.07f) },
            new TradeEntry[0]));

        cfg.enchantments.put("replant", tableEnchCfg(enchCfg(
            new LootEntry[]{ loot("minecraft:chests/village/*_house", 1, 0.20f) },
            new TradeEntry[]{ villager("minecraft:farmer", 2, 1, 14, 3) })));

        cfg.enchantments.put("magnetic", tableEnchCfg(enchCfg(
            new LootEntry[]{ loot("minecraft:chests/end_city_treasure",  1, 0.15f),
                             loot("minecraft:chests/stronghold_corridor", 1, 0.10f),
                             loot("minecraft:chests/stronghold_crossing", 1, 0.10f),
                             loot("minecraft:chests/stronghold_library",  1, 0.10f) },
            new TradeEntry[0])));

        cfg.enchantments.put("flame_walker", enchCfg(
            new LootEntry[]{ loot("minecraft:chests/nether_bridge",    1, 0.10f),
                             loot("minecraft:chests/bastion_treasure", 2, 0.10f) },
            new TradeEntry[0]));

        List<LootEntry> xpLoot = new ArrayList<>();
        for (String t : new String[]{
                "minecraft:chests/simple_dungeon",      "minecraft:chests/abandoned_mineshaft",
                "minecraft:chests/jungle_temple",       "minecraft:chests/desert_pyramid",
                "minecraft:chests/igloo_chest",         "minecraft:chests/pillager_outpost",
                "minecraft:chests/buried_treasure",     "minecraft:chests/woodland_mansion",
                "minecraft:chests/nether_bridge",       "minecraft:chests/bastion_treasure",
                "minecraft:chests/end_city_treasure",   "minecraft:chests/stronghold_corridor",
                "minecraft:chests/stronghold_crossing", "minecraft:chests/stronghold_library" }) {
            boolean hard = t.contains("end_city") || t.contains("woodland_mansion") || t.contains("bastion");
            xpLoot.add(loot(t, 1, 0.15f));
            xpLoot.add(loot(t, 2, 0.10f));
            if (hard) xpLoot.add(loot(t, 3, 0.07f));
        }
        EnchantmentConfig xpCfg = new EnchantmentConfig();
        xpCfg.loot = xpLoot;
        xpCfg.table_enchantable = true;
        xpCfg.trades_enabled = false;
        cfg.enchantments.put("experience", xpCfg);

        cfg.enchantments.put("treecapitator", enchCfg(
            new LootEntry[]{ loot("minecraft:chests/woodland_mansion", 1, 0.15f) },
            new TradeEntry[]{ wanderer(2, 1, 20, 1) }));

        cfg.enchantments.put("vein_miner", enchCfg(
            new LootEntry[]{ loot("minecraft:chests/abandoned_mineshaft", 1, 0.12f),
                             loot("minecraft:chests/ancient_city",         1, 0.20f) },
            new TradeEntry[0]));

        cfg.enchantments.put("auto_smelt", enchCfg(
            new LootEntry[]{ loot("minecraft:chests/nether_bridge",       1, 0.15f),
                             loot("minecraft:chests/bastion*",            1, 0.15f),
                             loot("minecraft:chests/abandoned_mineshaft", 1, 0.12f),
                             loot("minecraft:gameplay/piglin_bartering",  1, 0.08f) },
            new TradeEntry[0]));

        cfg.enchantments.put("double_shot", tableEnchCfg(enchCfg(
            new LootEntry[]{ loot("minecraft:chests/pillager_outpost", 1, 0.18f) },
            new TradeEntry[]{ villager("minecraft:fletcher", 3, 1, 18, 2) })));

        cfg.enchantments.put("triple_shot", enchCfg(
            new LootEntry[]{ loot("minecraft:chests/end_city_treasure", 1, 0.15f) },
            new TradeEntry[0]));

        cfg.enchantments.put("homing", enchCfg(
            new LootEntry[]{ loot("minecraft:chests/stronghold_corridor", 1, 0.12f),
                             loot("minecraft:chests/stronghold_crossing",  1, 0.12f),
                             loot("minecraft:chests/stronghold_library",   1, 0.12f) },
            new TradeEntry[]{ villager("minecraft:fletcher", 5, 1, 24, 1) }));

        List<LootEntry> evasionLoot = new ArrayList<>();
        for (String t : new String[]{
                "minecraft:chests/simple_dungeon",      "minecraft:chests/abandoned_mineshaft",
                "minecraft:chests/jungle_temple",       "minecraft:chests/desert_pyramid",
                "minecraft:chests/igloo_chest",         "minecraft:chests/pillager_outpost",
                "minecraft:chests/buried_treasure",     "minecraft:chests/woodland_mansion",
                "minecraft:chests/nether_bridge",       "minecraft:chests/bastion_treasure",
                "minecraft:chests/end_city_treasure",   "minecraft:chests/stronghold_corridor",
                "minecraft:chests/stronghold_crossing", "minecraft:chests/stronghold_library",
                "minecraft:chests/ancient_city" }) {
            evasionLoot.add(loot(t, 1, 0.12f));
        }
        EnchantmentConfig evasionCfg = new EnchantmentConfig();
        evasionCfg.loot = evasionLoot;
        evasionCfg.table_enchantable = true;
        evasionCfg.trades_enabled = false;
        cfg.enchantments.put("evasion", evasionCfg);

        return cfg;
    }

    // ── Builder helpers ───────────────────────────────────────────────────────

    private static EnchantmentConfig enchCfg(LootEntry[] loot, TradeEntry[] trades) {
        EnchantmentConfig c = new EnchantmentConfig();
        c.loot   = new ArrayList<>(Arrays.asList(loot));
        c.trades = new ArrayList<>(Arrays.asList(trades));
        if (trades.length == 0) c.trades_enabled = false;
        return c;
    }

    private static EnchantmentConfig tableEnchCfg(EnchantmentConfig c) {
        c.table_enchantable = true;
        return c;
    }

    private static LootEntry loot(String table, int level, float chance) {
        LootEntry e = new LootEntry(); e.table = table; e.level = level; e.chance = chance; return e;
    }

    private static TradeEntry villager(String profession, int villagerLevel, int enchLevel, int cost, int maxUses) {
        TradeEntry e = new TradeEntry();
        e.type = "villager"; e.profession = profession; e.villager_level = villagerLevel;
        e.ench_level = enchLevel; e.cost = cost; e.max_uses = maxUses; return e;
    }

    private static TradeEntry wanderer(int tier, int enchLevel, int cost, int maxUses) {
        TradeEntry e = new TradeEntry();
        e.type = "wandering_trader"; e.tier = tier;
        e.ench_level = enchLevel; e.cost = cost; e.max_uses = maxUses; return e;
    }
}
