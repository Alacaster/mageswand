package com.firstmage.mageswand.wand;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public record WandContext(
        Plugin plugin,
        Player player,
        ItemStack wand,
        ItemStack offhandFuel,
        WandInteractionType interactionType,
        Location playerLocation,
        Location eyeLocation,
        Vector lookDirection,
        WandTargetType targetType,
        Entity targetEntity,
        Block targetBlock,
        Location interactionPoint,
        Location targetLocation,
        double distanceToTarget,
        double baseEntityRange,
        double baseBlockRange,
        boolean withinBaseEntityRange,
        boolean withinBaseBlockRange,
        long createdAtMillis
) {
    public boolean isFirstMageUser() {
        return WandConstants.FIRST_MAGE_NAME.equalsIgnoreCase(this.player.getName());
    }

    public boolean isTargetingFirstMage() {
        return this.targetEntity instanceof Player target
                && WandConstants.FIRST_MAGE_NAME.equalsIgnoreCase(target.getName());
    }

    public boolean isLeftClick() {
        return switch (this.interactionType) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK, LEFT_CLICK_ENTITY_ATTACK -> true;
            default -> false;
        };
    }

    public boolean isRightClick() {
        return !this.isLeftClick();
    }

    public boolean isBlockInteraction() {
        return this.interactionType == WandInteractionType.LEFT_CLICK_BLOCK
                || this.interactionType == WandInteractionType.RIGHT_CLICK_BLOCK;
    }

    public boolean isEntityInteraction() {
        return this.interactionType == WandInteractionType.LEFT_CLICK_ENTITY_ATTACK
                || this.interactionType == WandInteractionType.RIGHT_CLICK_ENTITY_USE;
    }
}