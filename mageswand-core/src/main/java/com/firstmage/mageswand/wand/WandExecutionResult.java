package com.firstmage.mageswand.wand;

public record WandExecutionResult(
        int fuelCost,
        boolean triggerNoFuelFallback,
        boolean retryAsUntargetedInteraction,
        boolean allowVanillaBlockUse,
        boolean allowVanillaItemUse,
        boolean allowVanillaEntityAttack,
        boolean allowVanillaEntityUse,
        boolean allowExtendedVanillaBlockInteractions
) {
    private static final WandExecutionResult PASS_THROUGH = new WandExecutionResult(0, false, false, true, true, true, true, false);
    private static final WandExecutionResult DENY_ALL = new WandExecutionResult(0, false, false, false, false, false, false, false);
    private static final WandExecutionResult NO_FUEL_FALLBACK = new WandExecutionResult(0, true, false, true, true, true, true, false);
    private static final WandExecutionResult RETRY_AS_UNTARGETED = new WandExecutionResult(0, false, true, false, false, false, false, false);

    public WandExecutionResult {
        if (fuelCost < 0) {
            throw new IllegalArgumentException("fuelCost cannot be negative");
        }
    }

    public static WandExecutionResult passThrough() {
        return PASS_THROUGH;
    }

    public static WandExecutionResult denyAll() {
        return DENY_ALL;
    }

    public static WandExecutionResult noFuelFallback() {
        return NO_FUEL_FALLBACK;
    }

    public static WandExecutionResult retryAsUntargeted() {
        return RETRY_AS_UNTARGETED;
    }

    public static WandExecutionResult consumeAndDenyAll(int fuelCost) {
        return new WandExecutionResult(fuelCost, false, false, false, false, false, false, false);
    }

    public static WandExecutionResult consumeAndPassThrough(int fuelCost) {
        return new WandExecutionResult(fuelCost, false, false, true, true, true, true, false);
    }

    public static WandExecutionResult of(
            int fuelCost,
            boolean allowVanillaBlockUse,
            boolean allowVanillaItemUse,
            boolean allowVanillaEntityAttack,
            boolean allowVanillaEntityUse
    ) {
        return new WandExecutionResult(
                fuelCost,
                false,
                false,
                allowVanillaBlockUse,
                allowVanillaItemUse,
                allowVanillaEntityAttack,
                allowVanillaEntityUse,
                false
        );
    }

    public WandExecutionResult withFuelCost(int fuelCost) {
        return new WandExecutionResult(
                fuelCost,
                this.triggerNoFuelFallback,
                this.retryAsUntargetedInteraction,
                this.allowVanillaBlockUse,
                this.allowVanillaItemUse,
                this.allowVanillaEntityAttack,
                this.allowVanillaEntityUse,
                this.allowExtendedVanillaBlockInteractions
        );
    }

    public WandExecutionResult withTriggerNoFuelFallback(boolean triggerNoFuelFallback) {
        return new WandExecutionResult(
                this.fuelCost,
                triggerNoFuelFallback,
                this.retryAsUntargetedInteraction,
                this.allowVanillaBlockUse,
                this.allowVanillaItemUse,
                this.allowVanillaEntityAttack,
                this.allowVanillaEntityUse,
                this.allowExtendedVanillaBlockInteractions
        );
    }

    public WandExecutionResult withRetryAsUntargetedInteraction(boolean retryAsUntargetedInteraction) {
        return new WandExecutionResult(
                this.fuelCost,
                this.triggerNoFuelFallback,
                retryAsUntargetedInteraction,
                this.allowVanillaBlockUse,
                this.allowVanillaItemUse,
                this.allowVanillaEntityAttack,
                this.allowVanillaEntityUse,
                this.allowExtendedVanillaBlockInteractions
        );
    }

    public WandExecutionResult withAllowVanillaBlockUse(boolean allowVanillaBlockUse) {
        return new WandExecutionResult(
                this.fuelCost,
                this.triggerNoFuelFallback,
                this.retryAsUntargetedInteraction,
                allowVanillaBlockUse,
                this.allowVanillaItemUse,
                this.allowVanillaEntityAttack,
                this.allowVanillaEntityUse,
                this.allowExtendedVanillaBlockInteractions
        );
    }

    public WandExecutionResult withAllowVanillaItemUse(boolean allowVanillaItemUse) {
        return new WandExecutionResult(
                this.fuelCost,
                this.triggerNoFuelFallback,
                this.retryAsUntargetedInteraction,
                this.allowVanillaBlockUse,
                allowVanillaItemUse,
                this.allowVanillaEntityAttack,
                this.allowVanillaEntityUse,
                this.allowExtendedVanillaBlockInteractions
        );
    }

    public WandExecutionResult withAllowVanillaEntityAttack(boolean allowVanillaEntityAttack) {
        return new WandExecutionResult(
                this.fuelCost,
                this.triggerNoFuelFallback,
                this.retryAsUntargetedInteraction,
                this.allowVanillaBlockUse,
                this.allowVanillaItemUse,
                allowVanillaEntityAttack,
                this.allowVanillaEntityUse,
                this.allowExtendedVanillaBlockInteractions
        );
    }

    public WandExecutionResult withAllowVanillaEntityUse(boolean allowVanillaEntityUse) {
        return new WandExecutionResult(
                this.fuelCost,
                this.triggerNoFuelFallback,
                this.retryAsUntargetedInteraction,
                this.allowVanillaBlockUse,
                this.allowVanillaItemUse,
                this.allowVanillaEntityAttack,
                allowVanillaEntityUse,
                this.allowExtendedVanillaBlockInteractions
        );
    }

    public WandExecutionResult withAllowExtendedVanillaBlockInteractions(boolean allowExtendedVanillaBlockInteractions) {
        return new WandExecutionResult(
                this.fuelCost,
                this.triggerNoFuelFallback,
                this.retryAsUntargetedInteraction,
                this.allowVanillaBlockUse,
                this.allowVanillaItemUse,
                this.allowVanillaEntityAttack,
                this.allowVanillaEntityUse,
                allowExtendedVanillaBlockInteractions
        );
    }
}
