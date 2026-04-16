package com.firstmage.mageswand.wand;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public final class WandTargeting {
    private WandTargeting() {
    }

    public static WandContext createLeftClickAirContext(Plugin plugin, Player player) {
        return createAirContext(plugin, player, WandInteractionType.LEFT_CLICK_AIR);
    }

    public static WandContext createRightClickAirContext(Plugin plugin, Player player) {
        return createAirContext(plugin, player, WandInteractionType.RIGHT_CLICK_AIR);
    }

    public static WandContext createLeftClickBlockContext(Plugin plugin, Player player, Block block, @Nullable Location interactionPoint) {
        return createBlockContext(plugin, player, WandInteractionType.LEFT_CLICK_BLOCK, block, interactionPoint);
    }

    public static WandContext createRightClickBlockContext(Plugin plugin, Player player, Block block, @Nullable Location interactionPoint) {
        return createBlockContext(plugin, player, WandInteractionType.RIGHT_CLICK_BLOCK, block, interactionPoint);
    }

    public static WandContext createLeftClickEntityContext(Plugin plugin, Player player, Entity entity) {
        return createEntityContext(plugin, player, WandInteractionType.LEFT_CLICK_ENTITY_ATTACK, entity, null);
    }

    public static WandContext createRightClickEntityContext(Plugin plugin, Player player, Entity entity, @Nullable Location interactionPoint) {
        return createEntityContext(plugin, player, WandInteractionType.RIGHT_CLICK_ENTITY_USE, entity, interactionPoint);
    }

    public static boolean isWithinBaseBlockRange(Player player, Block block) {
        double baseBlockRange = getBaseRange(player, Attribute.BLOCK_INTERACTION_RANGE, 4.5D);
        Location hitLocation = nearestPoint(player.getEyeLocation(), block.getBoundingBox(), player.getWorld());
        return player.getEyeLocation().distance(hitLocation) <= baseBlockRange;
    }

    public static boolean isWithinBaseEntityRange(Player player, Entity entity) {
        double baseEntityRange = getBaseRange(player, Attribute.ENTITY_INTERACTION_RANGE, 3.0D);
        Location hitLocation = nearestPoint(player.getEyeLocation(), entity.getBoundingBox(), player.getWorld());
        return player.getEyeLocation().distance(hitLocation) <= baseEntityRange;
    }

    private static WandContext createAirContext(Plugin plugin, Player player, WandInteractionType interactionType) {
        Snapshot snapshot = snapshot(player);
        Location targetLocation = snapshot.eyeLocation().clone().add(snapshot.lookDirection().clone().multiply(snapshot.baseBlockRange()));

        return new WandContext(
                plugin,
                player,
                snapshot.mainHand(),
                snapshot.offHand(),
                interactionType,
                snapshot.playerLocation(),
                snapshot.eyeLocation(),
                snapshot.lookDirection(),
                WandTargetType.MISS,
                null,
                null,
                null,
                targetLocation,
                0.0D,
                snapshot.baseEntityRange(),
                snapshot.baseBlockRange(),
                true,
                true,
                System.currentTimeMillis()
        );
    }

    private static WandContext createBlockContext(Plugin plugin, Player player, WandInteractionType interactionType, Block block, @Nullable Location interactionPoint) {
        Snapshot snapshot = snapshot(player);
        Location targetLocation = interactionPoint == null
                ? nearestPoint(snapshot.eyeLocation(), block.getBoundingBox(), player.getWorld())
                : interactionPoint.clone();
        double distanceToTarget = snapshot.eyeLocation().distance(targetLocation);
        boolean withinBaseBlockRange = distanceToTarget <= snapshot.baseBlockRange();

        return new WandContext(
                plugin,
                player,
                snapshot.mainHand(),
                snapshot.offHand(),
                interactionType,
                snapshot.playerLocation(),
                snapshot.eyeLocation(),
                snapshot.lookDirection(),
                WandTargetType.BLOCK,
                null,
                block,
                interactionPoint == null ? null : interactionPoint.clone(),
                targetLocation,
                distanceToTarget,
                snapshot.baseEntityRange(),
                snapshot.baseBlockRange(),
                true,
                withinBaseBlockRange,
                System.currentTimeMillis()
        );
    }

    private static WandContext createEntityContext(Plugin plugin, Player player, WandInteractionType interactionType, Entity entity, @Nullable Location interactionPoint) {
        Snapshot snapshot = snapshot(player);
        Location targetLocation = interactionPoint == null
                ? nearestPoint(snapshot.eyeLocation(), entity.getBoundingBox(), player.getWorld())
                : interactionPoint.clone();
        double distanceToTarget = snapshot.eyeLocation().distance(targetLocation);
        boolean withinBaseEntityRange = distanceToTarget <= snapshot.baseEntityRange();

        return new WandContext(
                plugin,
                player,
                snapshot.mainHand(),
                snapshot.offHand(),
                interactionType,
                snapshot.playerLocation(),
                snapshot.eyeLocation(),
                snapshot.lookDirection(),
                WandTargetType.ENTITY,
                entity,
                null,
                interactionPoint == null ? null : interactionPoint.clone(),
                targetLocation,
                distanceToTarget,
                snapshot.baseEntityRange(),
                snapshot.baseBlockRange(),
                withinBaseEntityRange,
                true,
                System.currentTimeMillis()
        );
    }

    public static @Nullable Location entityInteractionPoint(Entity entity, @Nullable Vector clickedPosition) {
        if (clickedPosition == null) {
            return null;
        }
        return entity.getLocation().clone().add(clickedPosition);
    }

    private static Snapshot snapshot(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand().clone();
        ItemStack offHand = player.getInventory().getItemInOffHand().clone();
        Location playerLocation = player.getLocation();
        Location eyeLocation = player.getEyeLocation();
        Vector lookDirection = eyeLocation.getDirection().normalize();
        double baseEntityRange = getBaseRange(player, Attribute.ENTITY_INTERACTION_RANGE, 3.0D);
        double baseBlockRange = getBaseRange(player, Attribute.BLOCK_INTERACTION_RANGE, 4.5D);
        return new Snapshot(mainHand, offHand, playerLocation, eyeLocation, lookDirection, baseEntityRange, baseBlockRange);
    }

    private static Location nearestPoint(Location from, BoundingBox box, World world) {
        double x = clamp(from.getX(), box.getMinX(), box.getMaxX());
        double y = clamp(from.getY(), box.getMinY(), box.getMaxY());
        double z = clamp(from.getZ(), box.getMinZ(), box.getMaxZ());
        return new Location(world, x, y, z);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double getBaseRange(Player player, Attribute attribute, double fallback) {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance == null ? fallback : instance.getBaseValue();
    }

    private record Snapshot(
            ItemStack mainHand,
            ItemStack offHand,
            Location playerLocation,
            Location eyeLocation,
            Vector lookDirection,
            double baseEntityRange,
            double baseBlockRange
    ) {
    }
}
