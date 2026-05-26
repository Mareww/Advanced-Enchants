package net.marewmod.advancedenchants;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class AdvancedEnchantsConfigScreen extends Screen {

        private static final ItemStack ENCH_TABLE_ICON = new ItemStack(Items.ENCHANTING_TABLE);
    private static final ItemStack TRADE_ICON      = new ItemStack(Items.EMERALD);

    private final Screen parent;

    // Parallel lists tracking icon positions for rendering
    private final List<String> iconEnchIds    = new ArrayList<>();
    private final List<int[]>  tableIconPos   = new ArrayList<>(); // {x, y}
    private final List<int[]>  tradeIconPos   = new ArrayList<>(); // {x, y}

    public AdvancedEnchantsConfigScreen(Screen parent) {
        super(Text.literal("Advanced Enchants Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        iconEnchIds.clear();
        tableIconPos.clear();
        tradeIconPos.clear();

        AdvancedEnchantsConfig cfg = AdvancedEnchantsConfig.get();
        int cx = this.width / 2;
        int y = 30;

        for (Map.Entry<String, AdvancedEnchantsConfig.EnchantmentConfig> entry : cfg.enchantments.entrySet()) {
            String id = entry.getKey();
            AdvancedEnchantsConfig.EnchantmentConfig enchCfg = entry.getValue();
            Enchantment ench = Registries.ENCHANTMENT.get(new Identifier(AdvancedEnchantsMod.MOD_ID, id));
            Text label = ench != null ? Text.translatable(ench.getTranslationKey()) : Text.literal(prettify(id));

            // ── Trade icon (emerald) — left of toggle ────────────────────────
            int tradeX = cx - 124;
            int tableX = cx + 64;
            iconEnchIds.add(id);
            tradeIconPos.add(new int[]{ tradeX, y });
            tableIconPos.add(new int[]{ tableX, y });

            addDrawableChild(ButtonWidget.builder(Text.empty(), btn -> {
                enchCfg.trades_enabled = !enchCfg.trades_enabled;
            }).dimensions(tradeX, y, 20, 20).build());

            // ── On/Off toggle (160px wide) ───────────────────────────────────
            addDrawableChild(CyclingButtonWidget.<Boolean>builder(
                    v -> Text.literal(v ? "On" : "Off").formatted(v ? Formatting.GREEN : Formatting.RED))
                .values(List.of(true, false))
                .initially(enchCfg.enabled)
                .build(cx - 100, y, 160, 20, label, (btn, v) -> enchCfg.enabled = v));

            // ── Enchanting table icon button — right of toggle ────────────────
            addDrawableChild(ButtonWidget.builder(Text.empty(), btn -> {
                enchCfg.table_enchantable = !enchCfg.table_enchantable;
                clearAndInit();
            }).dimensions(tableX, y, 20, 20).build());

            // ── Rarity cycling button — only when table is enabled ────────────
            if (enchCfg.table_enchantable) {
                Enchantment enchObj = Registries.ENCHANTMENT.get(new Identifier(AdvancedEnchantsMod.MOD_ID, id));
                int currentWeight = enchCfg.weight > 0 ? enchCfg.weight
                                  : (enchObj != null ? enchObj.getRarity().getWeight() : 5);
                addDrawableChild(CyclingButtonWidget.<Integer>builder(w -> switch (w) {
                        case 10 -> Text.literal("Common").formatted(Formatting.WHITE);
                        case 5  -> Text.literal("Uncommon").formatted(Formatting.GREEN);
                        case 2  -> Text.literal("Rare").formatted(Formatting.AQUA);
                        default -> Text.literal("Very Rare").formatted(Formatting.LIGHT_PURPLE);
                    })
                    .values(List.of(10, 5, 2, 1))
                    .initially(currentWeight)
                    .build(cx + 88, y, 80, 20, Text.empty(), (btn, w) -> enchCfg.weight = w));
            }

            y += 24;
        }

        y += 10;
        addDrawableChild(ButtonWidget.builder(Text.literal("Edit Loot Tables"), btn -> {
            AdvancedEnchantsConfig.save();
            client.setScreen(new AdvancedEnchantsLootScreen(this));
        }).dimensions(cx - 100, y, 200, 20).build());
        y += 30;

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
            .dimensions(cx - 50, y, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        super.render(ctx, mouseX, mouseY, delta);

        // Draw enchanting table + trade icons over their buttons
        AdvancedEnchantsConfig cfg = AdvancedEnchantsConfig.get();
        for (int i = 0; i < iconEnchIds.size(); i++) {
            AdvancedEnchantsConfig.EnchantmentConfig ec = cfg.enchantments.get(iconEnchIds.get(i));

            int[] tp = tableIconPos.get(i);
            ctx.drawItemWithoutEntity(ENCH_TABLE_ICON, tp[0] + 2, tp[1] + 2);
            boolean tableOn = ec != null && ec.table_enchantable;
            ctx.fill(tp[0] + 1, tp[1] + 1, tp[0] + 19, tp[1] + 19, tableOn ? 0x4400CC00 : 0x44CC0000);

            int[] tr = tradeIconPos.get(i);
            ctx.drawItemWithoutEntity(TRADE_ICON, tr[0] + 2, tr[1] + 2);
            boolean tradeOn = ec == null || ec.trades_enabled;
            ctx.fill(tr[0] + 1, tr[1] + 1, tr[0] + 19, tr[1] + 19, tradeOn ? 0x4400CC00 : 0x44CC0000);
        }
    }

    @Override
    public void close() {
        AdvancedEnchantsConfig.save();
        IntegratedServer server = client.getServer();
        if (server != null) {
            server.execute(() -> {
                for (var player : server.getPlayerManager().getPlayerList()) {
                    boolean stripped = false;
                    for (int i = 0; i < player.getInventory().size(); i++) {
                        if (AdvancedEnchantsConfig.stripDisabled(player.getInventory().getStack(i)))
                            stripped = true;
                    }
                    if (stripped) {
                        player.getInventory().markDirty();
                        player.playerScreenHandler.sendContentUpdates();
                        player.sendMessage(Text.literal(
                            "[Advanced Enchants] Some disabled enchantments were removed from your items."));
                    }
                }
                var cm = server.getCommandManager();
                cm.execute(cm.getDispatcher().parse("reload", server.getCommandSource()), "reload");
            });
        } else if (client.player != null) {
            client.player.sendMessage(Text.literal(
                "[Advanced Enchants] Config saved. Ask the server admin to run /reload."));
        }
        client.setScreen(parent);
    }

    private static String prettify(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        for (String word : s.split("_")) {
            if (!word.isEmpty()) { sb.append(Character.toUpperCase(word.charAt(0))); sb.append(word.substring(1)); sb.append(' '); }
        }
        return sb.toString().trim();
    }
}
