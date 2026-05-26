package net.marewmod.advancedenchantments;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdvancedEnchantmentsLootScreen extends Screen {

    private static class LootRow {
        String table, level, chance;
        LootRow(String table, String level, String chance) {
            this.table = table; this.level = level; this.chance = chance;
        }
    }

    private final Screen parent;
    private final List<String> enchIds = new ArrayList<>();
    private final List<Text>   enchLabels = new ArrayList<>();
    private final List<List<LootRow>> pages = new ArrayList<>();

    private int expandedIdx = -1; // which enchantment is currently open

    // Fields for the expanded section only
    private final List<TextFieldWidget> tableFields  = new ArrayList<>();
    private final List<TextFieldWidget> levelFields  = new ArrayList<>();
    private final List<TextFieldWidget> chanceFields = new ArrayList<>();

    // Layout (set in init, read in render)
    private int headerY  = 0;
    private int startX   = 0;
    private int levelX   = 0;
    private int chanceX  = 0;

    public AdvancedEnchantmentsLootScreen(Screen parent) {
        super(Text.literal("Loot Table Settings"));
        this.parent = parent;
        AdvancedEnchantmentsConfig cfg = AdvancedEnchantmentsConfig.get();
        for (Map.Entry<String, AdvancedEnchantmentsConfig.EnchantmentConfig> entry : cfg.enchantments.entrySet()) {
            enchIds.add(entry.getKey());
            Enchantment ench = Registries.ENCHANTMENT.get(
                new Identifier(AdvancedEnchantmentsMod.MOD_ID, entry.getKey()));
            enchLabels.add(ench != null ? Text.translatable(ench.getTranslationKey())
                                       : Text.literal(entry.getKey()));
            List<LootRow> rows = new ArrayList<>();
            for (AdvancedEnchantmentsConfig.LootEntry le : entry.getValue().loot)
                rows.add(new LootRow(le.table, String.valueOf(le.level), String.valueOf(Math.round(le.chance * 100))));
            pages.add(rows);
        }
    }

    @Override
    protected void init() {
        tableFields.clear();
        levelFields.clear();
        chanceFields.clear();

        int cx     = this.width / 2;
        int tableW = 180, levelW = 35, chanceW = 50, xW = 20, gap = 4;
        int totalW = tableW + gap + levelW + gap + chanceW + gap + xW;
        startX     = cx - totalW / 2;
        levelX     = startX + tableW + gap;
        chanceX    = levelX + levelW + gap;
        int xBtnX  = chanceX + chanceW + gap;

        int btnW = Math.min(totalW, 240);
        int btnX = cx - btnW / 2;

        int y = 20;

        for (int i = 0; i < enchIds.size(); i++) {
            final int idx = i;
            boolean open = (expandedIdx == i);

            // ── Enchantment header button ────────────────────────────────────
            Text prefix = Text.literal(open ? "▼ " : "► ").formatted(open ? Formatting.YELLOW : Formatting.GRAY);
            Text label  = Text.literal("").append(prefix).append(enchLabels.get(i));
            addDrawableChild(ButtonWidget.builder(label, btn -> {
                syncFields();
                expandedIdx = (expandedIdx == idx) ? -1 : idx;
                clearAndInit();
            }).dimensions(btnX, y, btnW, 20).build());
            y += 24;

            if (!open) continue;

            // ── Column headers Y (stored for render) ─────────────────────────
            headerY = y;
            y += 14;

            // ── Loot entry rows ──────────────────────────────────────────────
            List<LootRow> rows = pages.get(i);
            for (int r = 0; r < rows.size(); r++) {
                LootRow row = rows.get(r);

                TextFieldWidget tf = new TextFieldWidget(textRenderer, startX, y, tableW, 18, Text.empty());
                tf.setMaxLength(200);
                tf.setText(row.table);
                addDrawableChild(tf);
                tableFields.add(tf);

                TextFieldWidget lf = new TextFieldWidget(textRenderer, levelX, y, levelW, 18, Text.empty());
                lf.setMaxLength(5);
                lf.setText(row.level);
                addDrawableChild(lf);
                levelFields.add(lf);

                TextFieldWidget cf = new TextFieldWidget(textRenderer, chanceX, y, chanceW, 18, Text.empty());
                cf.setMaxLength(10);
                cf.setText(row.chance);
                addDrawableChild(cf);
                chanceFields.add(cf);

                final int ridx = r;
                addDrawableChild(ButtonWidget.builder(Text.literal("X"), btn -> {
                    syncFields();
                    pages.get(expandedIdx).remove(ridx);
                    clearAndInit();
                }).dimensions(xBtnX, y, xW, 18).build());

                y += 22;
            }

            // ── [+] add row ──────────────────────────────────────────────────
            addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> {
                syncFields();
                pages.get(expandedIdx).add(new LootRow("", "1", "0.10"));
                clearAndInit();
            }).dimensions(startX, y, 24, 18).build());
            y += 30;
        }

        y += 4;
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
            .dimensions(cx - 50, y, 100, 20).build());
    }

    private void syncFields() {
        if (expandedIdx < 0 || expandedIdx >= pages.size()) return;
        List<LootRow> rows = pages.get(expandedIdx);
        for (int i = 0; i < tableFields.size() && i < rows.size(); i++) {
            LootRow r = rows.get(i);
            r.table  = tableFields.get(i).getText().trim();
            r.level  = levelFields.get(i).getText().trim();
            r.chance = chanceFields.get(i).getText().trim();
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 6, 0xFFFFFF);

        if (expandedIdx >= 0) {
            ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("Table").formatted(Formatting.YELLOW), startX, headerY, 0xFFFFFF);
            ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("Lvl").formatted(Formatting.YELLOW), levelX, headerY, 0xFFFFFF);
            ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("Chance %").formatted(Formatting.YELLOW), chanceX, headerY, 0xFFFFFF);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        syncFields();
        AdvancedEnchantmentsConfig cfg = AdvancedEnchantmentsConfig.get();
        for (int p = 0; p < enchIds.size(); p++) {
            AdvancedEnchantmentsConfig.EnchantmentConfig enchCfg = cfg.enchantments.get(enchIds.get(p));
            if (enchCfg == null) continue;
            List<AdvancedEnchantmentsConfig.LootEntry> newLoot = new ArrayList<>();
            for (LootRow row : pages.get(p)) {
                if (row.table.isBlank()) continue;
                AdvancedEnchantmentsConfig.LootEntry le = new AdvancedEnchantmentsConfig.LootEntry();
                le.table = row.table;
                try { le.level  = Integer.parseInt(row.level);  } catch (NumberFormatException e) { le.level  = 1;     }
                try { le.chance = Float.parseFloat(row.chance) / 100f; } catch (NumberFormatException e) { le.chance = 0.10f; }
                newLoot.add(le);
            }
            enchCfg.loot = newLoot;
        }
        AdvancedEnchantmentsConfig.save();
        client.setScreen(parent);
    }
}
