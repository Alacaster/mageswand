package com.firstmage.mageswand.wand;

import org.bukkit.entity.Player;

/**
 * Debug sink only.
 *
 * Callers are expected to guard calls with:
 *   if (WandBuildFlags.DEBUG) { ... }
 *
 * That way the whole call site disappears in non-debug builds.
 */
public final class WandDebug {
    private static final String PREFIX = "[WAND DEBUG] ";

    private WandDebug() {
    }

    public static void chat(Player player, String message) {
        player.sendMessage(PREFIX + message);
    }

    public static void chat(WandContext context, String message) {
        chat(context.player(), message);
    }
}
