package com.firstmage.mageswand.wand;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class WandClientBiomes {
    private static volatile Adapter ADAPTER = Adapter.unavailable();

    private WandClientBiomes() {
    }

    public static boolean isAvailable() {
        return ADAPTER.isAvailable();
    }

    public static void spoofForDuration(Player player, NamespacedKey biomeKey, long durationTicks) {
        ADAPTER.spoofForDuration(player, biomeKey, durationTicks);
    }

    public static void clear(Player player) {
        ADAPTER.clear(player);
    }

    public static void install(Adapter adapter) {
        ADAPTER = Objects.requireNonNull(adapter, "adapter");
    }

    public interface Adapter {
        boolean isAvailable();

        void spoofForDuration(Player player, NamespacedKey biomeKey, long durationTicks);

        void clear(Player player);

        static Adapter unavailable() {
            return new Adapter() {
                @Override
                public boolean isAvailable() {
                    return false;
                }

                @Override
                public void spoofForDuration(Player player, NamespacedKey biomeKey, long durationTicks) {
                }

                @Override
                public void clear(Player player) {
                }
            };
        }
    }
}
