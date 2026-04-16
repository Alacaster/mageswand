package com.firstmage.mageswand.wand.executor;

import com.firstmage.mageswand.wand.WandConstants;
import com.firstmage.mageswand.wand.WandContext;
import com.firstmage.mageswand.wand.WandExecutionResult;
import com.firstmage.mageswand.wand.WandExecutor;
import com.firstmage.mageswand.wand.WandInteractionType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class AmethystShardWandExecutor implements WandExecutor {
    private static final int NORMAL_LEFT_COST = 1;
    private static final double NORMAL_LEFT_FREE_CAST_CHANCE = 0.5D;
    private static final int NORMAL_RIGHT_COST = 1;
    private static final double NORMAL_RIGHT_FREE_CAST_CHANCE = 0.5D;
    private static final int FIRST_MAGE_LEFT_COST = 1;
    private static final double FIRST_MAGE_LEFT_FREE_CAST_CHANCE = 0.5D;
    private static final int FIRST_MAGE_RIGHT_COST = 1;
    private static final double FIRST_MAGE_RIGHT_FREE_CAST_CHANCE = 0.5D;
    private static final int AGAINST_FIRST_MAGE_LEFT_COST = 2;
    private static final double AGAINST_FIRST_MAGE_LEFT_FREE_CAST_CHANCE = 0.5D;
    private static final int AGAINST_FIRST_MAGE_RIGHT_COST = 2;
    private static final double AGAINST_FIRST_MAGE_RIGHT_FREE_CAST_CHANCE = 0.5D;

    // Push ranges. These define the actual visible line length for push casts.
    private static final double NORMAL_PUSH_RANGE = 8.5D;
    private static final double FIRST_MAGE_PUSH_RANGE = 10.25D;
    private static final double AGAINST_FIRST_MAGE_PUSH_RANGE = 14.5D;

    // Push cylinder thickness.
    private static final double NORMAL_PUSH_SELECTION_RADIUS = 1.30D;
    private static final double FIRST_MAGE_PUSH_SELECTION_RADIUS = 1.65D;
    private static final double AGAINST_FIRST_MAGE_PUSH_SELECTION_RADIUS = 1.95D;

    // Pull ranges.
    private static final double NORMAL_PULL_RANGE = 8.5D;
    private static final double FIRST_MAGE_PULL_RANGE = 10.25D;
    private static final double AGAINST_FIRST_MAGE_PULL_RANGE = 18.5D;

    private static final double AMETHYST_MOTION_SCALAR = 0.5D;

    // Push strengths.
    private static final double BASE_NORMAL_PUSH_STRENGTH = 0.28D;
    private static final double BASE_FIRST_MAGE_PUSH_STRENGTH = 0.46D;
    private static final double BASE_AGAINST_FIRST_MAGE_PUSH_STRENGTH = 0.85D;

    private static final double NORMAL_PUSH_STRENGTH = BASE_NORMAL_PUSH_STRENGTH * AMETHYST_MOTION_SCALAR;
    private static final double FIRST_MAGE_PUSH_STRENGTH = BASE_FIRST_MAGE_PUSH_STRENGTH * AMETHYST_MOTION_SCALAR;
    private static final double AGAINST_FIRST_MAGE_PUSH_STRENGTH = BASE_AGAINST_FIRST_MAGE_PUSH_STRENGTH * AMETHYST_MOTION_SCALAR;

    // Pull strengths.
    private static final double BASE_NORMAL_PULL_STRENGTH = 0.3D;
    private static final double BASE_FIRST_MAGE_PULL_STRENGTH = 0.42D;
    private static final double BASE_AGAINST_FIRST_MAGE_PULL_STRENGTH = 0.62D;

    private static final double NORMAL_PULL_STRENGTH = BASE_NORMAL_PULL_STRENGTH * AMETHYST_MOTION_SCALAR;
    private static final double FIRST_MAGE_PULL_STRENGTH = BASE_FIRST_MAGE_PULL_STRENGTH * AMETHYST_MOTION_SCALAR;
    private static final double AGAINST_FIRST_MAGE_PULL_STRENGTH = BASE_AGAINST_FIRST_MAGE_PULL_STRENGTH * AMETHYST_MOTION_SCALAR;

    // Push vertical tuning.
    private static final double BASE_NORMAL_PUSH_VERTICAL_DIRECTIONAL_MULTIPLIER = 1.00D;
    private static final double BASE_FIRST_MAGE_PUSH_VERTICAL_DIRECTIONAL_MULTIPLIER = 1.05D;
    private static final double BASE_AGAINST_FIRST_MAGE_PUSH_VERTICAL_DIRECTIONAL_MULTIPLIER = 1.32D;

    private static final double NORMAL_PUSH_VERTICAL_DIRECTIONAL_MULTIPLIER = BASE_NORMAL_PUSH_VERTICAL_DIRECTIONAL_MULTIPLIER * AMETHYST_MOTION_SCALAR;
    private static final double FIRST_MAGE_PUSH_VERTICAL_DIRECTIONAL_MULTIPLIER = BASE_FIRST_MAGE_PUSH_VERTICAL_DIRECTIONAL_MULTIPLIER * AMETHYST_MOTION_SCALAR;
    private static final double AGAINST_FIRST_MAGE_PUSH_VERTICAL_DIRECTIONAL_MULTIPLIER = BASE_AGAINST_FIRST_MAGE_PUSH_VERTICAL_DIRECTIONAL_MULTIPLIER * AMETHYST_MOTION_SCALAR;

    private static final double BASE_NORMAL_PUSH_VERTICAL_FLAT_BOOST = 0.39D;
    private static final double BASE_FIRST_MAGE_PUSH_VERTICAL_FLAT_BOOST = 0.52D;
    private static final double BASE_AGAINST_FIRST_MAGE_PUSH_VERTICAL_FLAT_BOOST = 0.70D;

    private static final double NORMAL_PUSH_VERTICAL_FLAT_BOOST = BASE_NORMAL_PUSH_VERTICAL_FLAT_BOOST * AMETHYST_MOTION_SCALAR;
    private static final double FIRST_MAGE_PUSH_VERTICAL_FLAT_BOOST = BASE_FIRST_MAGE_PUSH_VERTICAL_FLAT_BOOST * AMETHYST_MOTION_SCALAR;
    private static final double AGAINST_FIRST_MAGE_PUSH_VERTICAL_FLAT_BOOST = BASE_AGAINST_FIRST_MAGE_PUSH_VERTICAL_FLAT_BOOST * AMETHYST_MOTION_SCALAR;

    // Pull vertical tuning.
    private static final double BASE_NORMAL_PULL_VERTICAL_DIRECTIONAL_MULTIPLIER = 0.92D;
    private static final double BASE_FIRST_MAGE_PULL_VERTICAL_DIRECTIONAL_MULTIPLIER = 1.04D;
    private static final double BASE_AGAINST_FIRST_MAGE_PULL_VERTICAL_DIRECTIONAL_MULTIPLIER = 1.30D;

    private static final double NORMAL_PULL_VERTICAL_DIRECTIONAL_MULTIPLIER = BASE_NORMAL_PULL_VERTICAL_DIRECTIONAL_MULTIPLIER * AMETHYST_MOTION_SCALAR;
    private static final double FIRST_MAGE_PULL_VERTICAL_DIRECTIONAL_MULTIPLIER = BASE_FIRST_MAGE_PULL_VERTICAL_DIRECTIONAL_MULTIPLIER * AMETHYST_MOTION_SCALAR;
    private static final double AGAINST_FIRST_MAGE_PULL_VERTICAL_DIRECTIONAL_MULTIPLIER = BASE_AGAINST_FIRST_MAGE_PULL_VERTICAL_DIRECTIONAL_MULTIPLIER * AMETHYST_MOTION_SCALAR;

    private static final double BASE_NORMAL_PULL_VERTICAL_FLAT_BOOST = 0.36D;
    private static final double BASE_FIRST_MAGE_PULL_VERTICAL_FLAT_BOOST = 0.5D;
    private static final double BASE_AGAINST_FIRST_MAGE_PULL_VERTICAL_FLAT_BOOST = 0.65D;

    private static final double NORMAL_PULL_VERTICAL_FLAT_BOOST = BASE_NORMAL_PULL_VERTICAL_FLAT_BOOST * AMETHYST_MOTION_SCALAR;
    private static final double FIRST_MAGE_PULL_VERTICAL_FLAT_BOOST = BASE_FIRST_MAGE_PULL_VERTICAL_FLAT_BOOST * AMETHYST_MOTION_SCALAR;
    private static final double AGAINST_FIRST_MAGE_PULL_VERTICAL_FLAT_BOOST = BASE_AGAINST_FIRST_MAGE_PULL_VERTICAL_FLAT_BOOST * AMETHYST_MOTION_SCALAR;

    // Blink distances.
    private static final double NORMAL_BLINK_DISTANCE = 5.0D;
    private static final double FIRST_MAGE_BLINK_DISTANCE = 8.0D;
    private static final double SUPER_BLINK_MULTIPLIER = 2.75D;
    private static final int BLINK_VERTICAL_SCAN = 16;

    // Blink fatigue model.
    private static final double BLINK_FATIGUE_PER_TELEPORT = 0.34D;
    private static final double BLINK_FATIGUE_THRESHOLD = 1.0D;
    private static final double BLINK_FATIGUE_OVERFLOW_VALUE = 2.0D;
    private static final double BLINK_LOW_FATIGUE_THRESHOLD = 0.02D;
    private static final double BLINK_FATIGUE_HALF_LIFE_SECONDS = 7D;
    private static final double BLINK_RESET_CHANCE_AT_LOW_FATIGUE = 0.9D;
    private static final double SUPER_BLINK_CHANCE_AT_LOW_FATIGUE = 0.12D;

    private static final int NORMAL_BUFF_DURATION_TICKS = 20 * 4;
    private static final int FIRST_MAGE_BUFF_DURATION_TICKS = 20 * 4;

    private final Map<UUID, BlinkState> blinkStates = new HashMap<>();

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
        return handleLeftClickEntity(context);
    }

    @Override
    public WandExecutionResult onRightClickAir(WandContext context) {
        return handleTeleport(context);
    }

    @Override
    public WandExecutionResult onRightClickBlock(WandContext context) {
        return handleTeleport(context);
    }

    @Override
    public WandExecutionResult onRightClickEntityUse(WandContext context) {
        return handleRightClickEntity(context);
    }

    private WandExecutionResult handleLeftClick(WandContext context) {
        FuelCostProfile costProfile = getUntargetedPushCostProfile(context);
        if (!hasEnoughFuel(context, costProfile)) {
            return WandExecutionResult.noFuelFallback();
        }

        castUntargetedPush(context);
        return WandExecutionResult.consumeAndDenyAll(resolveFuelCost(costProfile));
    }

    private WandExecutionResult handleLeftClickEntity(WandContext context) {
        Entity explicitTarget = context.targetEntity();
        if (explicitTarget == null || explicitTarget == context.player()) {
            return WandExecutionResult.retryAsUntargeted();
        }

        PushBeamProfile profile = chooseTargetedPushProfile(context, explicitTarget);
        if (context.distanceToTarget() > profile.range()) {
            return WandExecutionResult.retryAsUntargeted();
        }

        FuelCostProfile costProfile = getTargetedPushCostProfile(context, explicitTarget);
        if (!hasEnoughFuel(context, costProfile)) {
            return WandExecutionResult.noFuelFallback();
        }

        castTargetedPush(context, explicitTarget, profile);
        return WandExecutionResult.consumeAndDenyAll(resolveFuelCost(costProfile));
    }

    private WandExecutionResult handleRightClickEntity(WandContext context) {
        Entity target = context.targetEntity();
        if (target == null || target == context.player()) {
            return WandExecutionResult.retryAsUntargeted();
        }

        double maxRange = getPullRange(context, target);
        if (context.distanceToTarget() > maxRange) {
            return WandExecutionResult.retryAsUntargeted();
        }

        FuelCostProfile costProfile = getTargetedPullCostProfile(context, target);
        if (!hasEnoughFuel(context, costProfile)) {
            return WandExecutionResult.noFuelFallback();
        }

        castTargetedPull(context, target);
        return WandExecutionResult.consumeAndDenyAll(resolveFuelCost(costProfile));
    }

    private WandExecutionResult handleTeleport(WandContext context) {
        boolean firstMage = context.isFirstMageUser();
        double baseDistance = firstMage ? FIRST_MAGE_BLINK_DISTANCE : NORMAL_BLINK_DISTANCE;
        FuelCostProfile baseCostProfile = getUntargetedBlinkCostProfile(context);

        BlinkDecision decision = prepareBlinkDecision(context.player(), baseDistance, true);
        if (decision == null) {
            return WandExecutionResult.denyAll();
        }

        if (!hasEnoughFuel(context, baseCostProfile)) {
            return WandExecutionResult.noFuelFallback();
        }

        Location destination = findBestBlinkDestination(context.player(), decision.travelDirection(), decision.maxDistance());
        if (destination == null) {
            return WandExecutionResult.denyAll();
        }

        Player player = context.player();
        World world = player.getWorld();
        Location start = player.getLocation();

        if (firstMage) {
            world.spawnParticle(Particle.PORTAL, start.clone().add(0.0D, 1.0D, 0.0D), 35, 0.35D, 0.70D, 0.35D, 0.15D);
            player.teleport(destination);
            world.spawnParticle(Particle.WITCH, destination.clone().add(0.0D, 1.0D, 0.0D), 25, 0.35D, 0.70D, 0.35D, 0.03D);
            world.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.7F);

            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, FIRST_MAGE_BUFF_DURATION_TICKS, 1, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, FIRST_MAGE_BUFF_DURATION_TICKS, 0, true, true, true));
            world.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7F, 1.7F);
        } else {
            world.spawnParticle(Particle.PORTAL, start.clone().add(0.0D, 1.0D, 0.0D), 28, 0.30D, 0.65D, 0.30D, 0.12D);
            player.teleport(destination);
            world.spawnParticle(Particle.ENCHANT, destination.clone().add(0.0D, 1.0D, 0.0D), 24, 0.25D, 0.55D, 0.25D, 0.02D);
            world.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.75F, 1.9F);

            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, NORMAL_BUFF_DURATION_TICKS, 0, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, NORMAL_BUFF_DURATION_TICKS, 0, true, true, true));
            world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_HIT, 1.0F, 1.2F);
        }

        int resolvedFuelCost = decision.luckyReset() ? 0 : resolveFuelCost(baseCostProfile);
        applyPostTeleportFatigue(player, decision);
        return WandExecutionResult.consumeAndDenyAll(resolvedFuelCost);
    }

    private void castUntargetedPush(WandContext context) {
        World world = context.player().getWorld();
        Location origin = context.eyeLocation();
        Vector direction = normalizeOrFallback(context.lookDirection());

        PushBeamProfile profile = chooseUntargetedPushProfile(context);
        double beamLength = beamLengthForContext(context, profile.range());
        Set<Entity> hits = collectEntitiesOnBeam(context, origin, direction, beamLength, profile.selectionRadius());

        drawAndPlayPushBeam(world, origin, direction, beamLength, profile);
        applyPushProfile(context, hits, profile);
    }

    private void castTargetedPush(WandContext context, Entity explicitTarget, PushBeamProfile profile) {
        World world = context.player().getWorld();
        Location origin = context.eyeLocation();
        Vector direction = normalizeOrFallback(context.lookDirection());

        double beamLength = beamLengthForContext(context, profile.range());
        Set<Entity> hits = collectEntitiesOnBeam(context, origin, direction, beamLength, profile.selectionRadius());

        if (explicitTarget != null && explicitTarget != context.player() && context.distanceToTarget() <= beamLength) {
            hits.add(explicitTarget);
        }

        drawAndPlayPushBeam(world, origin, direction, beamLength, profile);
        applyPushProfile(context, hits, profile);
    }

    private void castTargetedPull(WandContext context, Entity target) {
        World world = context.player().getWorld();
        Location targetLoc = target.getLocation().add(0.0D, 1.0D, 0.0D);

        double strength = getPullStrength(context, target);
        Vector impulse = buildPullImpulse(context, target, context.playerLocation(), target.getLocation(), strength);
        addImpulse(target, impulse);

        if (target instanceof Player playerTarget) {
            playerTarget.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 4, 0, true, true, true));
            playerTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0, true, true, true));
        }

        world.spawnParticle(Particle.REVERSE_PORTAL, targetLoc, 28, 0.30D, 0.55D, 0.30D, 0.03D);
        world.spawnParticle(Particle.ENCHANT, targetLoc, 18, 0.25D, 0.45D, 0.25D, 0.02D);
        world.playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8F, isTargetingFirstMagePlayer(context, target) ? 1.25F : 1.55F);
    }

    private PushBeamProfile chooseUntargetedPushProfile(WandContext context) {
        if (context.isFirstMageUser()) {
            return PushBeamProfile.firstMageCaster();
        }

        double longBeamLength = beamLengthForContext(context, AGAINST_FIRST_MAGE_PUSH_RANGE);
        Set<Entity> longHits = collectEntitiesOnBeam(
                context,
                context.eyeLocation(),
                normalizeOrFallback(context.lookDirection()),
                longBeamLength,
                AGAINST_FIRST_MAGE_PUSH_SELECTION_RADIUS
        );

        if (containsFirstMage(longHits)) {
            return PushBeamProfile.againstFirstMage();
        }

        return PushBeamProfile.normal();
    }

    private PushBeamProfile chooseTargetedPushProfile(WandContext context, Entity explicitTarget) {
        if (context.isFirstMageUser()) {
            return PushBeamProfile.firstMageCaster();
        }

        if (isTargetingFirstMagePlayer(context, explicitTarget)) {
            return PushBeamProfile.againstFirstMage();
        }

        double longBeamLength = beamLengthForContext(context, AGAINST_FIRST_MAGE_PUSH_RANGE);
        Set<Entity> longHits = collectEntitiesOnBeam(
                context,
                context.eyeLocation(),
                normalizeOrFallback(context.lookDirection()),
                longBeamLength,
                AGAINST_FIRST_MAGE_PUSH_SELECTION_RADIUS
        );

        if (containsFirstMage(longHits)) {
            return PushBeamProfile.againstFirstMage();
        }

        return PushBeamProfile.normal();
    }

    private boolean containsFirstMage(Set<Entity> entities) {
        for (Entity entity : entities) {
            if (isFirstMagePlayer(entity)) {
                return true;
            }
        }
        return false;
    }

    private double beamLengthForContext(WandContext context, double desiredRange) {
        if (context.interactionType() == WandInteractionType.LEFT_CLICK_BLOCK && context.targetBlock() != null) {
            return Math.min(desiredRange, context.distanceToTarget());
        }
        return desiredRange;
    }

    private Set<Entity> collectEntitiesOnBeam(WandContext context, Location origin, Vector direction, double beamLength, double selectionRadius) {
        World world = context.player().getWorld();
        Set<Entity> hits = new LinkedHashSet<>();
        double search = beamLength + selectionRadius + 1.5D;

        for (Entity entity : world.getNearbyEntities(origin, search, search, search)) {
            if (entity == context.player()) {
                continue;
            }
            if (!isInsideBeamCylinder(origin, direction, entity.getLocation().add(0.0D, 1.0D, 0.0D), beamLength, selectionRadius)) {
                continue;
            }
            hits.add(entity);
        }
        return hits;
    }

    private void drawAndPlayPushBeam(World world, Location origin, Vector direction, double length, PushBeamProfile profile) {
        drawBeamLine(world, origin, direction, length, profile.primaryParticle(), profile.secondaryParticle());
        world.playSound(origin, profile.sound(), profile.volume(), profile.pitch());
    }

    private void applyPushProfile(WandContext context, Set<Entity> targets, PushBeamProfile profile) {
        for (Entity target : targets) {
            if (target == null || target == context.player()) {
                continue;
            }

            double strength = getPushStrength(context, target);
            Vector impulse = buildPushImpulse(context, target, context.playerLocation(), target.getLocation(), strength);
            addImpulse(target, impulse);

            if (target instanceof Player playerTarget) {
                playerTarget.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 4, 0, true, true, true));
            }

            World world = target.getWorld();
            Location targetLoc = target.getLocation().add(0.0D, 1.0D, 0.0D);
            world.spawnParticle(profile.primaryParticle(), targetLoc, 16, 0.25D, 0.35D, 0.25D, 0.01D);
            world.spawnParticle(profile.secondaryParticle(), targetLoc, 10, 0.25D, 0.35D, 0.25D, 0.02D);
        }
    }

    private VerticalProfile getPushVerticalProfile(WandContext context, Entity target) {
        if (isTargetingFirstMagePlayer(context, target)) {
            return new VerticalProfile(
                    AGAINST_FIRST_MAGE_PUSH_VERTICAL_DIRECTIONAL_MULTIPLIER,
                    AGAINST_FIRST_MAGE_PUSH_VERTICAL_FLAT_BOOST
            );
        }
        if (context.isFirstMageUser()) {
            return new VerticalProfile(
                    FIRST_MAGE_PUSH_VERTICAL_DIRECTIONAL_MULTIPLIER,
                    FIRST_MAGE_PUSH_VERTICAL_FLAT_BOOST
            );
        }
        return new VerticalProfile(
                NORMAL_PUSH_VERTICAL_DIRECTIONAL_MULTIPLIER,
                NORMAL_PUSH_VERTICAL_FLAT_BOOST
        );
    }

    private VerticalProfile getPullVerticalProfile(WandContext context, Entity target) {
        if (isTargetingFirstMagePlayer(context, target)) {
            return new VerticalProfile(
                    AGAINST_FIRST_MAGE_PULL_VERTICAL_DIRECTIONAL_MULTIPLIER,
                    AGAINST_FIRST_MAGE_PULL_VERTICAL_FLAT_BOOST
            );
        }
        if (context.isFirstMageUser()) {
            return new VerticalProfile(
                    FIRST_MAGE_PULL_VERTICAL_DIRECTIONAL_MULTIPLIER,
                    FIRST_MAGE_PULL_VERTICAL_FLAT_BOOST
            );
        }
        return new VerticalProfile(
                NORMAL_PULL_VERTICAL_DIRECTIONAL_MULTIPLIER,
                NORMAL_PULL_VERTICAL_FLAT_BOOST
        );
    }

    private record VerticalProfile(double directionalMultiplier, double flatBoost) {
    }

    private record FuelCostProfile(int amount, double freeCastChance) {
    }
    private Vector buildPushImpulse(WandContext context, Entity target, Location from, Location to, double strength) {
        Vector direction = to.toVector().subtract(from.toVector());
        if (direction.lengthSquared() < 1.0E-8D) {
            direction = new Vector(0.0D, 0.0D, 1.0D);
        }

        Vector impulse = direction.normalize().multiply(strength);
        VerticalProfile profile = getPushVerticalProfile(context, target);

        double directionalVertical = Math.max(impulse.getY(), 0.0D) * profile.directionalMultiplier();
        double boostedVertical = directionalVertical + profile.flatBoost();

        impulse.setY(boostedVertical);
        return impulse;
    }

    private Vector buildPullImpulse(WandContext context, Entity target, Location playerLocation, Location targetLocation, double horizontalStrength) {
        Vector vector = playerLocation.toVector().subtract(targetLocation.toVector());
        if (vector.lengthSquared() < 1.0E-8D) {
            vector = new Vector(0.0D, 0.0D, 1.0D);
        }

        Vector impulse = vector.normalize().multiply(horizontalStrength);
        VerticalProfile profile = getPullVerticalProfile(context, target);

        double directionalVertical = Math.max(impulse.getY(), 0.0D) * profile.directionalMultiplier();
        double boostedVertical = directionalVertical + profile.flatBoost();

        impulse.setY(boostedVertical);
        return impulse;
    }

    private void addImpulse(Entity target, Vector impulse) {
        target.setVelocity(target.getVelocity().add(impulse));
    }

    private double getPushStrength(WandContext context, Entity target) {
        if (isFirstMagePlayer(target)) {
            return AGAINST_FIRST_MAGE_PUSH_STRENGTH;
        }
        if (context.isFirstMageUser()) {
            return FIRST_MAGE_PUSH_STRENGTH;
        }
        return NORMAL_PUSH_STRENGTH;
    }

    private double getPullRange(WandContext context, Entity target) {
        if (isTargetingFirstMagePlayer(context, target)) {
            return AGAINST_FIRST_MAGE_PULL_RANGE;
        }
        if (context.isFirstMageUser()) {
            return FIRST_MAGE_PULL_RANGE;
        }
        return NORMAL_PULL_RANGE;
    }

    private double getPullStrength(WandContext context, Entity target) {
        if (isTargetingFirstMagePlayer(context, target)) {
            return AGAINST_FIRST_MAGE_PULL_STRENGTH;
        }
        if (context.isFirstMageUser()) {
            return FIRST_MAGE_PULL_STRENGTH;
        }
        return NORMAL_PULL_STRENGTH;
    }

    private FuelCostProfile getUntargetedPushCostProfile(WandContext context) {
        if (context.isFirstMageUser()) {
            return new FuelCostProfile(FIRST_MAGE_LEFT_COST, FIRST_MAGE_LEFT_FREE_CAST_CHANCE);
        }
        return new FuelCostProfile(NORMAL_LEFT_COST, NORMAL_LEFT_FREE_CAST_CHANCE);
    }

    private FuelCostProfile getUntargetedBlinkCostProfile(WandContext context) {
        if (context.isFirstMageUser()) {
            return new FuelCostProfile(FIRST_MAGE_RIGHT_COST, FIRST_MAGE_RIGHT_FREE_CAST_CHANCE);
        }
        return new FuelCostProfile(NORMAL_RIGHT_COST, NORMAL_RIGHT_FREE_CAST_CHANCE);
    }

    private FuelCostProfile getTargetedPushCostProfile(WandContext context, Entity target) {
        if (isTargetingFirstMagePlayer(context, target)) {
            return new FuelCostProfile(AGAINST_FIRST_MAGE_LEFT_COST, AGAINST_FIRST_MAGE_LEFT_FREE_CAST_CHANCE);
        }
        if (context.isFirstMageUser()) {
            return new FuelCostProfile(FIRST_MAGE_LEFT_COST, FIRST_MAGE_LEFT_FREE_CAST_CHANCE);
        }
        return new FuelCostProfile(NORMAL_LEFT_COST, NORMAL_LEFT_FREE_CAST_CHANCE);
    }

    private FuelCostProfile getTargetedPullCostProfile(WandContext context, Entity target) {
        if (isTargetingFirstMagePlayer(context, target)) {
            return new FuelCostProfile(AGAINST_FIRST_MAGE_RIGHT_COST, AGAINST_FIRST_MAGE_RIGHT_FREE_CAST_CHANCE);
        }
        if (context.isFirstMageUser()) {
            return new FuelCostProfile(FIRST_MAGE_RIGHT_COST, FIRST_MAGE_RIGHT_FREE_CAST_CHANCE);
        }
        return new FuelCostProfile(NORMAL_RIGHT_COST, NORMAL_RIGHT_FREE_CAST_CHANCE);
    }

    private boolean isTargetingFirstMagePlayer(WandContext context, Entity target) {
        return isFirstMagePlayer(target);
    }

    private boolean isFirstMagePlayer(Entity target) {
        return target instanceof Player player && WandConstants.FIRST_MAGE_NAME.equalsIgnoreCase(player.getName());
    }

    private BlinkDecision prepareBlinkDecision(Player player, double baseDistance, boolean allowBackwardWhenSneaking) {
        Vector direction = player.getLocation().getDirection().clone().setY(0.0D);
        if (direction.lengthSquared() < 1.0E-8D) {
            return null;
        }
        direction.normalize();

        if (allowBackwardWhenSneaking && player.isSneaking()) {
            direction.multiply(-1.0D);
        }

        BlinkState state = this.blinkStates.computeIfAbsent(player.getUniqueId(), ignored -> new BlinkState());
        long nowNanos = System.nanoTime();
        double decisionFatigue = decayedFatigue(state, nowNanos);

        if (decisionFatigue > BLINK_FATIGUE_THRESHOLD) {
            return null;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        boolean lowFatigue = decisionFatigue < BLINK_LOW_FATIGUE_THRESHOLD;
        boolean luckyReset = lowFatigue && random.nextDouble() < BLINK_RESET_CHANCE_AT_LOW_FATIGUE;
        boolean superBlink = lowFatigue && random.nextDouble() < SUPER_BLINK_CHANCE_AT_LOW_FATIGUE;

        double maxDistance = superBlink ? baseDistance * SUPER_BLINK_MULTIPLIER : baseDistance;
        return new BlinkDecision(direction, maxDistance, decisionFatigue, nowNanos, luckyReset);
    }

    private void applyPostTeleportFatigue(Player player, BlinkDecision decision) {
        BlinkState state = this.blinkStates.computeIfAbsent(player.getUniqueId(), ignored -> new BlinkState());

        double newFatigue = decision.postDecisionBaseFatigue() + BLINK_FATIGUE_PER_TELEPORT;
        if (newFatigue > BLINK_FATIGUE_THRESHOLD) {
            newFatigue = BLINK_FATIGUE_OVERFLOW_VALUE;
        }
        if (decision.luckyReset()) {
            newFatigue = 0.0D;
        }

        state.fatigue = newFatigue;
        state.lastTeleportNanos = decision.decisionTimeNanos();
    }

    private double decayedFatigue(BlinkState state, long nowNanos) {
        if (state.lastTeleportNanos == 0L) {
            return state.fatigue;
        }

        double elapsedSeconds = (nowNanos - state.lastTeleportNanos) / 1_000_000_000.0D;
        if (elapsedSeconds <= 0.0D) {
            return state.fatigue;
        }

        double multiplier = Math.pow(0.5D, elapsedSeconds / BLINK_FATIGUE_HALF_LIFE_SECONDS);
        return state.fatigue * multiplier;
    }

    private Location findBestBlinkDestination(Player player, Vector direction, double maxDistance) {
        Location start = player.getLocation();

        for (double d = maxDistance; d >= 1.0D; d -= 0.5D) {
            Location horizontalProbe = start.clone().add(direction.clone().multiply(d));
            Location bestAtThisDistance = findNearestSafeStandingLocation(horizontalProbe, start);
            if (bestAtThisDistance != null) {
                bestAtThisDistance.setYaw(start.getYaw());
                bestAtThisDistance.setPitch(start.getPitch());
                return bestAtThisDistance;
            }
        }

        return null;
    }

    private Location findNearestSafeStandingLocation(Location horizontalProbe, Location originalStart) {
        World world = horizontalProbe.getWorld();
        int blockX = horizontalProbe.getBlockX();
        int blockZ = horizontalProbe.getBlockZ();
        int baseY = horizontalProbe.getBlockY();

        for (int offset = 0; offset <= BLINK_VERTICAL_SCAN; offset++) {
            Location same = testFeetLocation(world, blockX, baseY + offset, blockZ, originalStart);
            if (same != null) {
                return same;
            }

            if (offset == 0) {
                continue;
            }

            Location lower = testFeetLocation(world, blockX, baseY - offset, blockZ, originalStart);
            if (lower != null) {
                return lower;
            }
        }

        return null;
    }

    private Location testFeetLocation(World world, int blockX, int feetBlockY, int blockZ, Location originalStart) {
        int minFeetY = world.getMinHeight();
        int maxFeetY = world.getMaxHeight() - 2;
        if (feetBlockY < minFeetY || feetBlockY > maxFeetY) {
            return null;
        }

        Location feet = new Location(world, blockX + 0.5D, feetBlockY, blockZ + 0.5D, originalStart.getYaw(), originalStart.getPitch());
        return isSafeBlinkFeet(feet) ? feet : null;
    }

    private boolean isSafeBlinkFeet(Location feet) {
        Block support = feet.clone().add(0.0D, -1.0D, 0.0D).getBlock();
        Block feetBlock = feet.getBlock();
        Block headBlock = feet.clone().add(0.0D, 1.0D, 0.0D).getBlock();

        if (!isStandableSupport(support)) {
            return false;
        }
        if (!isOpenForBody(feetBlock) || !isOpenForBody(headBlock)) {
            return false;
        }

        return !isHazardous(support) && !isHazardous(feetBlock) && !isHazardous(headBlock);
    }

    private boolean isStandableSupport(Block block) {
        if (isNormalSupport(block)) {
            return true;
        }

        Material type = block.getType();
        return (type.name().endsWith("_CARPET") || type == Material.SNOW) && isNormalSupport(block.getRelative(0, -1, 0));
    }

    private boolean isNormalSupport(Block block) {
        return !block.isPassable() && !block.isLiquid() && !isHazardous(block);
    }

    private boolean isOpenForBody(Block block) {
        return block.isPassable() && !block.isLiquid() && !isHazardous(block);
    }

    private boolean isHazardous(Block block) {
        return switch (block.getType()) {
            case LAVA, FIRE, SOUL_FIRE, CAMPFIRE, SOUL_CAMPFIRE, CACTUS, MAGMA_BLOCK, SWEET_BERRY_BUSH, WITHER_ROSE, POWDER_SNOW -> true;
            default -> false;
        };
    }

    private void drawBeamLine(World world, Location start, Vector direction, double range, Particle primary, Particle secondary) {
        for (double d = 0.4D; d <= range; d += 0.35D) {
            Location point = start.clone().add(direction.clone().multiply(d));
            world.spawnParticle(primary, point, 2, 0.03D, 0.03D, 0.03D, 0.0D);
            world.spawnParticle(secondary, point, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    private boolean isInsideBeamCylinder(Location origin, Vector direction, Location point, double maxRange, double radius) {
        Vector toPoint = point.toVector().subtract(origin.toVector());
        double along = toPoint.dot(direction);
        if (along < 0.0D || along > maxRange) {
            return false;
        }

        Vector closestPoint = origin.toVector().add(direction.clone().multiply(along));
        return point.toVector().distance(closestPoint) <= radius;
    }

    private Vector normalizeOrFallback(Vector vector) {
        if (vector == null || vector.lengthSquared() < 1.0E-8D) {
            return new Vector(0.0D, 0.0D, 1.0D);
        }
        return vector.clone().normalize();
    }

    private int resolveFuelCost(FuelCostProfile profile) {
        if (profile.amount() <= 0) {
            return 0;
        }
        if (profile.freeCastChance() <= 0.0D) {
            return profile.amount();
        }
        return ThreadLocalRandom.current().nextDouble() < profile.freeCastChance() ? 0 : profile.amount();
    }

    private boolean hasEnoughFuel(WandContext context, FuelCostProfile profile) {
        return hasEnoughFuel(context, profile.amount());
    }

    private boolean hasEnoughFuel(WandContext context, int requiredAmount) {
        if (requiredAmount <= 0) {
            return true;
        }

        ItemStack offHand = context.player().getInventory().getItemInOffHand();
        return !offHand.getType().isAir() && offHand.getAmount() >= requiredAmount;
    }

    private static void debug(WandContext context, String message) {
    }

    private static void debug(Player player, String message) {
    }

    private static final class BlinkState {
        private double fatigue = 0.0D;
        private long lastTeleportNanos = 0L;
    }

    private record BlinkDecision(
            Vector travelDirection,
            double maxDistance,
            double postDecisionBaseFatigue,
            long decisionTimeNanos,
            boolean luckyReset
    ) {
    }

    private record PushBeamProfile(
            double range,
            double selectionRadius,
            double pushStrength,
            Particle primaryParticle,
            Particle secondaryParticle,
            Sound sound,
            float volume,
            float pitch
    ) {
        private static PushBeamProfile normal() {
            return new PushBeamProfile(
                    NORMAL_PUSH_RANGE,
                    NORMAL_PUSH_SELECTION_RADIUS,
                    NORMAL_PUSH_STRENGTH,
                    Particle.END_ROD,
                    Particle.ENCHANT,
                    Sound.BLOCK_AMETHYST_BLOCK_RESONATE,
                    1.0F,
                    1.45F
            );
        }

        private static PushBeamProfile firstMageCaster() {
            return new PushBeamProfile(
                    FIRST_MAGE_PUSH_RANGE,
                    FIRST_MAGE_PUSH_SELECTION_RADIUS,
                    FIRST_MAGE_PUSH_STRENGTH,
                    Particle.WITCH,
                    Particle.END_ROD,
                    Sound.BLOCK_AMETHYST_CLUSTER_BREAK,
                    1.0F,
                    0.75F
            );
        }

        private static PushBeamProfile againstFirstMage() {
            return new PushBeamProfile(
                    AGAINST_FIRST_MAGE_PUSH_RANGE,
                    AGAINST_FIRST_MAGE_PUSH_SELECTION_RADIUS,
                    AGAINST_FIRST_MAGE_PUSH_STRENGTH,
                    Particle.WITCH,
                    Particle.END_ROD,
                    Sound.BLOCK_AMETHYST_CLUSTER_BREAK,
                    1.0F,
                    0.75F
            );
        }
    }
}
