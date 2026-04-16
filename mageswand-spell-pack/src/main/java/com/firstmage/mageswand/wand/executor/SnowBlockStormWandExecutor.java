package com.firstmage.mageswand.wand.executor;

import com.firstmage.mageswand.wand.WandClientBiomes;
import com.firstmage.mageswand.wand.WandContext;
import com.firstmage.mageswand.wand.WandExecutionResult;
import com.firstmage.mageswand.wand.WandExecutor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public final class SnowBlockStormWandExecutor implements WandExecutor {
    private static final int FUEL_COST = 1;
    private static final long DURATION_TICKS = 20L * 8L;
    private static final NamespacedKey SPOOFED_BIOME = NamespacedKey.minecraft("snowy_taiga");

    @Override
    public WandExecutionResult onRightClickAir(WandContext context) {
        return cast(context);
    }

    @Override
    public WandExecutionResult onRightClickBlock(WandContext context) {
        return cast(context);
    }

    @Override
    public WandExecutionResult onRightClickEntityUse(WandContext context) {
        return cast(context);
    }

    private WandExecutionResult cast(WandContext context) {
        if (!hasEnoughFuel(context, FUEL_COST)) {
            return WandExecutionResult.noFuelFallback();
        }

        Player player = context.player();
        player.setPlayerWeather(WeatherType.DOWNFALL);
        player.playSound(player.getLocation(), Sound.WEATHER_RAIN_ABOVE, 0.8F, 1.15F);

        if (WandClientBiomes.isAvailable()) {
            WandClientBiomes.spoofForDuration(player, SPOOFED_BIOME, DURATION_TICKS);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.resetPlayerWeather();
                }
            }
        }.runTaskLater(context.plugin(), DURATION_TICKS);

        return WandExecutionResult.consumeAndDenyAll(FUEL_COST);
    }

    private static boolean hasEnoughFuel(WandContext context, int requiredAmount) {
        if (requiredAmount <= 0) {
            return true;
        }

        ItemStack offHand = context.player().getInventory().getItemInOffHand();
        return offHand.getType() == Material.SNOW_BLOCK && offHand.getAmount() >= requiredAmount;
    }
}
