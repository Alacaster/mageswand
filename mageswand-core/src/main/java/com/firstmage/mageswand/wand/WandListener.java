package com.firstmage.mageswand.wand;

import com.firstmage.mageswand.MagesWand;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WandListener implements Listener {
    private final MagesWand plugin;
    private final FuelRegistry fuelRegistry;
    private final Map<UUID, Long> denyInventoryOpenUntilTick = new HashMap<>();
    private final Map<UUID, DeniedBlockFollowup> deniedBlockFollowups = new HashMap<>();
    private final Map<UUID, ExtendedBlockGrant> extendedBlockGrants = new HashMap<>();
    private final Map<UUID, RecentEntityUse> recentEntityUses = new HashMap<>();
    private final Set<ClickEventIdentity> clicksToSwallow = new HashSet<>();

    public WandListener(MagesWand plugin, FuelRegistry fuelRegistry) {
        this.plugin = plugin;
        this.fuelRegistry = fuelRegistry;
    }

    public void clear() {
        this.denyInventoryOpenUntilTick.clear();
        this.deniedBlockFollowups.clear();
        this.extendedBlockGrants.clear();
        this.recentEntityUses.clear();
        this.clicksToSwallow.clear();
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isHoldingWand(player)) {
            clearBlockFollowupState(player);
            return;
        }

        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            handleOffHandInteractCompanion(event, player);
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR) {
            clearBlockFollowupState(player);
        }

        if (isGenericRightClick(event) && shouldSwallowGenericRightClickCompanion(player, event.getHand())) {
            if (WandBuildFlags.DEBUG) {
                WandDebug.chat(player, "Swallowed generic right-click companion after entity-use dispatch.");
            }
            denyInteract(event);
            return;
        }

        WandContext context = switch (event.getAction()) {
            case LEFT_CLICK_AIR -> WandTargeting.createLeftClickAirContext(this.plugin, player);
            case LEFT_CLICK_BLOCK -> event.getClickedBlock() == null
                    ? WandTargeting.createLeftClickAirContext(this.plugin, player)
                    : WandTargeting.createLeftClickBlockContext(this.plugin, player, event.getClickedBlock(), event.getInteractionPoint());
            case RIGHT_CLICK_AIR -> WandTargeting.createRightClickAirContext(this.plugin, player);
            case RIGHT_CLICK_BLOCK -> event.getClickedBlock() == null
                    ? WandTargeting.createRightClickAirContext(this.plugin, player)
                    : WandTargeting.createRightClickBlockContext(this.plugin, player, event.getClickedBlock(), event.getInteractionPoint());
            default -> null;
        };

        if (context == null) {
            return;
        }

        WandExecutionResult result = executeWandAction(context);
        result = retryAsUntargetedPlayerInteract(player, context, result);
        applyInteractResult(event, context, result);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onPrePlayerAttackEntity(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        if (!isHoldingWand(player)) {
            return;
        }

        WandContext context = WandTargeting.createLeftClickEntityContext(this.plugin, player, event.getAttacked());
        WandExecutionResult result = executeWandAction(context);

        if (result.retryAsUntargetedInteraction()) {
            if (WandBuildFlags.DEBUG) {
                WandDebug.chat(player, "Entity attack target declined by executor, retrying as left-click air.");
            }
            result = executeWandAction(WandTargeting.createLeftClickAirContext(this.plugin, player));
        }

        boolean allowVanillaAttack = context.withinBaseEntityRange()
                && event.willAttack()
                && result.allowVanillaEntityAttack();

        if (!allowVanillaAttack) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (!isHoldingWand(player)) {
            return;
        }

        if (shouldSkipDuplicateEntityUse(player, event.getRightClicked(), event.getHand())) {
            if (WandBuildFlags.DEBUG) {
                WandDebug.chat(player, "Skipped duplicate PlayerInteractEntityEvent/PlayerInteractAtEntityEvent pair.");
            }
            return;
        }

        markGenericRightClickCompanionToSwallow(player, event.getHand());

        WandContext context = WandTargeting.createRightClickEntityContext(this.plugin, player, event.getRightClicked(), null);
        WandExecutionResult result = executeWandAction(context);

        if (result.retryAsUntargetedInteraction()) {
            if (WandBuildFlags.DEBUG) {
                WandDebug.chat(player, "Entity use target declined by executor, retrying as right-click air.");
            }
            result = executeWandAction(WandTargeting.createRightClickAirContext(this.plugin, player));
        }

        boolean allowVanillaUse = context.withinBaseEntityRange()
                && result.allowVanillaEntityUse();

        if (!allowVanillaUse) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (!isHoldingWand(player)) {
            return;
        }

        if (shouldSkipDuplicateEntityUse(player, event.getRightClicked(), event.getHand())) {
            if (WandBuildFlags.DEBUG) {
                WandDebug.chat(player, "Skipped duplicate PlayerInteractAtEntityEvent/PlayerInteractEntityEvent pair.");
            }
            return;
        }

        markGenericRightClickCompanionToSwallow(player, event.getHand());

        WandContext context = WandTargeting.createRightClickEntityContext(
                this.plugin,
                player,
                event.getRightClicked(),
                WandTargeting.entityInteractionPoint(event.getRightClicked(), event.getClickedPosition())
        );
        WandExecutionResult result = executeWandAction(context);

        if (result.retryAsUntargetedInteraction()) {
            if (WandBuildFlags.DEBUG) {
                WandDebug.chat(player, "Precise entity use target declined by executor, retrying as right-click air.");
            }
            result = executeWandAction(WandTargeting.createRightClickAirContext(this.plugin, player));
        }

        boolean allowVanillaUse = context.withinBaseEntityRange()
                && result.allowVanillaEntityUse();

        if (!allowVanillaUse) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (!isHoldingWand(player)) {
            clearBlockFollowupState(player, event.getBlock());
            return;
        }

        if (hasExtendedMiningGrant(player, event.getBlock())) {
            return;
        }

        if (shouldDenyVanillaBlockInteraction(player, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        clearBlockFollowupState(event.getPlayer(), event.getBlock());
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isHoldingWand(player)) {
            clearBlockFollowupState(player, event.getBlock());
            return;
        }

        if (hasExtendedMiningGrant(player, event.getBlock())) {
            clearBlockFollowupState(player, event.getBlock());
            return;
        }

        if (shouldDenyVanillaBlockInteraction(player, event.getBlock())) {
            event.setCancelled(true);
            return;
        }

        clearBlockFollowupState(player, event.getBlock());
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (shouldDenyInventoryOpen(player)) {
            event.setCancelled(true);
        }
    }

    private void handleOffHandInteractCompanion(PlayerInteractEvent event, Player player) {
        if (shouldSwallowOffHandInteractCompanion(player, event.getHand())) {
            if (WandBuildFlags.DEBUG) {
                WandDebug.chat(player, "Swallowed off-hand interact companion after main-hand wand dispatch.");
            }
            denyInteract(event);
            return;
        }

        if (isGenericRightClick(event)
                && event.getClickedBlock() != null
                && !isWithinBaseRangeOrRightClickGranted(player, event.getClickedBlock())) {
            if (WandBuildFlags.DEBUG) {
                WandDebug.chat(player, "Denied off-hand block/item use outside vanilla block range while holding wand.");
            }
            denyInteract(event);
        }
    }

    private WandExecutionResult retryAsUntargetedPlayerInteract(Player player, WandContext context, WandExecutionResult result) {
        if (!result.retryAsUntargetedInteraction()) {
            return result;
        }

        return switch (context.interactionType()) {
            case LEFT_CLICK_BLOCK -> {
                if (WandBuildFlags.DEBUG) {
                    WandDebug.chat(player, "Block target declined by executor, retrying as left-click air.");
                }
                yield executeWandAction(WandTargeting.createLeftClickAirContext(this.plugin, player));
            }
            case RIGHT_CLICK_BLOCK -> {
                if (WandBuildFlags.DEBUG) {
                    WandDebug.chat(player, "Block target declined by executor, retrying as right-click air.");
                }
                yield executeWandAction(WandTargeting.createRightClickAirContext(this.plugin, player));
            }
            default -> result;
        };
    }

    private void applyInteractResult(PlayerInteractEvent event, WandContext context, WandExecutionResult result) {
        boolean blockInteraction = context.isBlockInteraction();
        Block targetBlock = context.targetBlock();
        boolean hasExtendedGrant = blockInteraction
                && targetBlock != null
                && result.allowExtendedVanillaBlockInteractions();
        boolean blockRangeAllowed = !blockInteraction || context.withinBaseBlockRange() || hasExtendedGrant;
        boolean allowVanillaBlockUse = blockRangeAllowed && result.allowVanillaBlockUse();
        boolean allowVanillaItemUse = blockRangeAllowed && result.allowVanillaItemUse();

        if (context.isRightClick() && (!allowVanillaBlockUse || !allowVanillaItemUse)) {
            markOffHandInteractCompanionToSwallow(context.player());
        }

        if (hasExtendedGrant) {
            registerExtendedBlockGrant(context.player(), targetBlock, context.interactionType(), allowVanillaBlockUse, allowVanillaItemUse);
        } else if (blockInteraction && targetBlock != null) {
            clearExtendedBlockGrant(context.player(), targetBlock);
        }

        if (blockInteraction && !allowVanillaBlockUse) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }

        if (!allowVanillaItemUse) {
            event.setUseItemInHand(Event.Result.DENY);
        }

        if (context.interactionType() == WandInteractionType.LEFT_CLICK_BLOCK && targetBlock != null) {
            if (!allowVanillaBlockUse || !allowVanillaItemUse) {
                markBlockFollowupDenied(context.player(), targetBlock);
            } else {
                clearDeniedBlockFollowup(context.player(), targetBlock);
            }
        }

        if (context.interactionType() == WandInteractionType.RIGHT_CLICK_BLOCK && !allowVanillaBlockUse) {
            markInventoryOpenDenied(context.player());
        }
    }

    private WandExecutionResult executeWandAction(WandContext context) {
        Player player = context.player();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        RegisteredFuel registeredFuel = this.fuelRegistry.findMatchingFuel(offHand);
        if (registeredFuel == null) {
            performNoFuelFallback(player);
            return WandExecutionResult.passThrough();
        }

        if (registeredFuel.operatorOnly() && !player.hasPermission(WandConstants.OP_ONLY_SPELL_PERMISSION)) {
            player.sendMessage(WandConstants.OP_ONLY_SPELL_MESSAGE);
            return WandExecutionResult.passThrough();
        }

        WandExecutionResult result = registeredFuel.executor().execute(context);
        if (result.triggerNoFuelFallback()) {
            performNoFuelFallback(player);
            return WandExecutionResult.passThrough();
        }

        if (result.fuelCost() > 0) {
            consumeFuel(player, result.fuelCost());
        }
        return result;
    }

    private void performNoFuelFallback(Player player) {
        player.damage(WandConstants.NO_FUEL_DAMAGE);
        player.sendMessage(WandConstants.NO_FUEL_MESSAGE);
    }

    private void consumeFuel(Player player, int amountToConsume) {
        if (amountToConsume <= 0) {
            return;
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand.getType().isAir()) {
            return;
        }

        int amount = offHand.getAmount();
        if (amount <= amountToConsume) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            return;
        }

        offHand.setAmount(amount - amountToConsume);
        player.getInventory().setItemInOffHand(offHand);
    }

    private boolean isHoldingWand(Player player) {
        return WandItems.isFirstMageWand(player.getInventory().getItemInMainHand());
    }

    private boolean isGenericRightClick(PlayerInteractEvent event) {
        return switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> true;
            default -> false;
        };
    }

    private void denyInteract(PlayerInteractEvent event) {
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
    }

    private boolean shouldDenyVanillaBlockInteraction(Player player, Block block) {
        if (isDeniedBlockFollowup(player, block)) {
            return true;
        }
        return !WandTargeting.isWithinBaseBlockRange(player, block);
    }

    private void registerExtendedBlockGrant(Player player, Block block, WandInteractionType interactionType, boolean allowVanillaBlockUse, boolean allowVanillaItemUse) {
        this.extendedBlockGrants.put(player.getUniqueId(), new ExtendedBlockGrant(
                BlockRef.of(block),
                interactionType,
                allowVanillaBlockUse,
                allowVanillaItemUse,
                currentServerTick()
        ));
    }

    private boolean hasExtendedMiningGrant(Player player, Block block) {
        ExtendedBlockGrant grant = this.extendedBlockGrants.get(player.getUniqueId());
        return grant != null
                && grant.interactionType == WandInteractionType.LEFT_CLICK_BLOCK
                && grant.block.equals(BlockRef.of(block))
                && (grant.allowVanillaBlockUse || grant.allowVanillaItemUse);
    }

    private boolean isWithinBaseRangeOrRightClickGranted(Player player, Block block) {
        if (WandTargeting.isWithinBaseBlockRange(player, block)) {
            return true;
        }

        ExtendedBlockGrant grant = this.extendedBlockGrants.get(player.getUniqueId());
        return grant != null
                && grant.interactionType == WandInteractionType.RIGHT_CLICK_BLOCK
                && grant.grantedAtTick == currentServerTick()
                && grant.block.equals(BlockRef.of(block))
                && (grant.allowVanillaBlockUse || grant.allowVanillaItemUse);
    }

    private void markBlockFollowupDenied(Player player, Block block) {
        this.deniedBlockFollowups.put(player.getUniqueId(), new DeniedBlockFollowup(BlockRef.of(block)));
    }

    private boolean isDeniedBlockFollowup(Player player, Block block) {
        DeniedBlockFollowup denied = this.deniedBlockFollowups.get(player.getUniqueId());
        return denied != null && denied.block.equals(BlockRef.of(block));
    }

    private void clearDeniedBlockFollowup(Player player, Block block) {
        DeniedBlockFollowup denied = this.deniedBlockFollowups.get(player.getUniqueId());
        if (denied != null && denied.block.equals(BlockRef.of(block))) {
            this.deniedBlockFollowups.remove(player.getUniqueId());
        }
    }

    private void clearExtendedBlockGrant(Player player, Block block) {
        ExtendedBlockGrant grant = this.extendedBlockGrants.get(player.getUniqueId());
        if (grant != null && grant.block.equals(BlockRef.of(block))) {
            this.extendedBlockGrants.remove(player.getUniqueId());
        }
    }

    private void clearBlockFollowupState(Player player, Block block) {
        clearDeniedBlockFollowup(player, block);
        clearExtendedBlockGrant(player, block);
    }

    private void clearBlockFollowupState(Player player) {
        this.deniedBlockFollowups.remove(player.getUniqueId());
        this.extendedBlockGrants.remove(player.getUniqueId());
    }

    private void markInventoryOpenDenied(Player player) {
        this.denyInventoryOpenUntilTick.put(player.getUniqueId(), currentServerTick() + 1L);
    }

    private boolean shouldDenyInventoryOpen(Player player) {
        Long expiryTick = this.denyInventoryOpenUntilTick.get(player.getUniqueId());
        if (expiryTick == null) {
            return false;
        }

        long currentTick = currentServerTick();
        if (currentTick <= expiryTick) {
            return true;
        }

        this.denyInventoryOpenUntilTick.remove(player.getUniqueId());
        return false;
    }

    private boolean shouldSkipDuplicateEntityUse(Player player, Entity entity, EquipmentSlot hand) {
        UUID playerId = player.getUniqueId();
        long tick = currentServerTick();
        RecentEntityUse previous = this.recentEntityUses.get(playerId);

        if (previous != null && previous.tick == tick && previous.entityId == entity.getEntityId() && previous.hand == hand) {
            return true;
        }

        this.recentEntityUses.put(playerId, new RecentEntityUse(tick, entity.getEntityId(), hand));
        return false;
    }

    private void markGenericRightClickCompanionToSwallow(Player player, EquipmentSlot hand) {
        pruneStaleClickSwallows();
        this.clicksToSwallow.add(new ClickEventIdentity(
                player.getUniqueId(),
                hand,
                currentServerTick(),
                ClickEventKind.GENERIC_RIGHT_CLICK_COMPANION
        ));
    }

    private boolean shouldSwallowGenericRightClickCompanion(Player player, EquipmentSlot hand) {
        pruneStaleClickSwallows();
        return this.clicksToSwallow.remove(new ClickEventIdentity(
                player.getUniqueId(),
                hand,
                currentServerTick(),
                ClickEventKind.GENERIC_RIGHT_CLICK_COMPANION
        ));
    }

    private void markOffHandInteractCompanionToSwallow(Player player) {
        pruneStaleClickSwallows();
        this.clicksToSwallow.add(new ClickEventIdentity(
                player.getUniqueId(),
                EquipmentSlot.OFF_HAND,
                currentServerTick(),
                ClickEventKind.OFF_HAND_INTERACT_COMPANION
        ));
    }

    private boolean shouldSwallowOffHandInteractCompanion(Player player, EquipmentSlot hand) {
        pruneStaleClickSwallows();
        return this.clicksToSwallow.remove(new ClickEventIdentity(
                player.getUniqueId(),
                hand,
                currentServerTick(),
                ClickEventKind.OFF_HAND_INTERACT_COMPANION
        ));
    }

    private void pruneStaleClickSwallows() {
        long currentTick = currentServerTick();
        this.clicksToSwallow.removeIf(identity -> identity.tick < currentTick);
    }

    private long currentServerTick() {
        return Bukkit.getCurrentTick();
    }

    private record RecentEntityUse(long tick, int entityId, EquipmentSlot hand) {
    }

    private record ClickEventIdentity(UUID playerId, EquipmentSlot hand, long tick, ClickEventKind kind) {
    }

    private enum ClickEventKind {
        GENERIC_RIGHT_CLICK_COMPANION,
        OFF_HAND_INTERACT_COMPANION
    }

    private record DeniedBlockFollowup(BlockRef block) {
    }

    private record ExtendedBlockGrant(
            BlockRef block,
            WandInteractionType interactionType,
            boolean allowVanillaBlockUse,
            boolean allowVanillaItemUse,
            long grantedAtTick
    ) {
    }

    private record BlockRef(UUID worldId, int x, int y, int z) {
        private static BlockRef of(Block block) {
            return new BlockRef(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }
}
