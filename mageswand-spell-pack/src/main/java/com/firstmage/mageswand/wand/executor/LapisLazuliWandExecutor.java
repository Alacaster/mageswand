package com.firstmage.mageswand.wand.executor;

import com.firstmage.mageswand.wand.WandContext;
import com.firstmage.mageswand.wand.WandExecutionResult;
import com.firstmage.mageswand.wand.WandExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class LapisLazuliWandExecutor implements WandExecutor {
    private static final int FUEL_COST = 2;
    private static final double NORMAL_PLAYER_FIRST_MAGE_RANGE = 10.0D;
    private static final int FIRST_MAGE_RESISTANCE_DURATION_TICKS = 20 * 5;
    private static final int FIRST_MAGE_RESISTANCE_AMPLIFIER = 2;
    private static final float NORMAL_PLAYER_FIRST_MAGE_FIREBALL_YIELD = 40.0F;
    private static final float NORMAL_PLAYER_OTHER_TARGET_FIREBALL_YIELD = 1.0F;
    private static final float FIRST_MAGE_FIREBALL_YIELD = 2.0F;
    private static final double FIREBALL_SPEED = 1.0D;

    @Override
    public WandExecutionResult onLeftClickAir(WandContext context) {
        return handleLeftClick(context);
    }

    @Override
    public WandExecutionResult onLeftClickBlock(WandContext context) {
        return handleLeftClick(context);
    }

    @Override
    public WandExecutionResult onLeftClickEntityAttack(WandContext context) {
        return handleLeftClick(context);
    }

    private WandExecutionResult handleLeftClick(WandContext context) {
        if (!hasEnoughFuel(context, FUEL_COST)) {
            return WandExecutionResult.noFuelFallback();
        }

        if (context.isFirstMageUser()) {
            return launchFireball(context, context.lookDirection(), FIRST_MAGE_FIREBALL_YIELD)
                    ? WandExecutionResult.consumeAndDenyAll(FUEL_COST)
                    : WandExecutionResult.passThrough();
        }

        Entity targetEntity = context.targetEntity();
        if (targetEntity instanceof Player targetPlayer) {
            if (!context.isTargetingFirstMage()) {
                return WandExecutionResult.passThrough();
            }

            if (context.distanceToTarget() > NORMAL_PLAYER_FIRST_MAGE_RANGE) {
                return WandExecutionResult.passThrough();
            }

            boolean launched = launchFireball(
                    context,
                    directionTowards(context.eyeLocation().toVector(), targetPlayer.getEyeLocation().toVector()),
                    NORMAL_PLAYER_FIRST_MAGE_FIREBALL_YIELD
            );
            if (!launched) {
                return WandExecutionResult.passThrough();
            }

            targetPlayer.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE,
                    FIRST_MAGE_RESISTANCE_DURATION_TICKS,
                    FIRST_MAGE_RESISTANCE_AMPLIFIER,
                    true,
                    true,
                    true
            ));
            return WandExecutionResult.consumeAndDenyAll(FUEL_COST);
        }

        return launchFireball(context, context.lookDirection(), NORMAL_PLAYER_OTHER_TARGET_FIREBALL_YIELD)
                ? WandExecutionResult.consumeAndDenyAll(FUEL_COST)
                : WandExecutionResult.passThrough();
    }

    private static boolean hasEnoughFuel(WandContext context, int requiredAmount) {
        if (requiredAmount <= 0) {
            return true;
        }

        ItemStack offHand = context.player().getInventory().getItemInOffHand();
        return !offHand.getType().isAir() && offHand.getAmount() >= requiredAmount;
    }

    private static boolean launchFireball(WandContext context, Vector rawDirection, float yield) {
        Vector direction = normalizeOrFallback(rawDirection, context.lookDirection());

        try {
            context.player().launchProjectile(
                    Fireball.class,
                    direction.clone().multiply(FIREBALL_SPEED),
                    fireball -> {
                        fireball.setShooter(context.player());
                        fireball.setYield(yield);
                        fireball.setIsIncendiary(false);
                        fireball.setDirection(direction);
                        fireball.setVelocity(direction.clone().multiply(FIREBALL_SPEED));
                    }
            );
            return true;
        } catch (Throwable throwable) {
            context.player().getServer().getLogger().warning(
                    "Failed to launch FirstMage wand fireball for " + context.player().getName() + ": " + throwable.getMessage()
            );
            return false;
        }
    }

    private static Vector directionTowards(Vector from, Vector to) {
        return to.clone().subtract(from);
    }

    private static Vector normalizeOrFallback(Vector primary, Vector fallback) {
        Vector chosen = isUsable(primary) ? primary.clone() : fallback.clone();
        if (!isUsable(chosen)) {
            chosen = new Vector(0.0D, 0.0D, 1.0D);
        }
        return chosen.normalize();
    }

    private static boolean isUsable(Vector vector) {
        return vector != null && vector.lengthSquared() > 1.0E-8D;
    }
}
