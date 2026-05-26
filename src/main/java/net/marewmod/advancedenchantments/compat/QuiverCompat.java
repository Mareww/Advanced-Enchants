package net.marewmod.advancedenchantments.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.reflect.Method;

public final class QuiverCompat {

    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("quiver");
    private static final Method CONSUME_EXTRA;
    private static final Method GET_AVAILABLE;

    static {
        Method consumeExtra = null, getAvailable = null;
        if (LOADED) {
            try {
                Class<?> tracker = Class.forName("net.marewmod.quiver.QuiverShotTracker");
                consumeExtra = tracker.getMethod("consumeExtra", PlayerEntity.class, int.class);
                getAvailable = tracker.getMethod("getAvailableCount");
            } catch (Exception ignored) {}
        }
        CONSUME_EXTRA = consumeExtra;
        GET_AVAILABLE = getAvailable;
    }

    /** Returns total arrows in the active quiver, or -1 if no quiver supplied the current shot. */
    public static int getAvailableCount() {
        if (!LOADED || GET_AVAILABLE == null) return -1;
        try {
            return (int) GET_AVAILABLE.invoke(null);
        } catch (Exception ignored) {
            return -1;
        }
    }

    /** Consumes {@code count} extra arrows from the active quiver. No-op if no quiver is active. */
    public static void consumeExtra(PlayerEntity player, int count) {
        if (!LOADED || CONSUME_EXTRA == null || count <= 0) return;
        try {
            CONSUME_EXTRA.invoke(null, player, count);
        } catch (Exception ignored) {}
    }

    private QuiverCompat() {}
}
