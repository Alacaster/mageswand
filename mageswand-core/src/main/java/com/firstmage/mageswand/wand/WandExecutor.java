package com.firstmage.mageswand.wand;

public interface WandExecutor {
    default WandExecutionResult execute(WandContext context) {
        return switch (context.interactionType()) {
            case LEFT_CLICK_AIR -> onLeftClickAir(context);
            case LEFT_CLICK_BLOCK -> onLeftClickBlock(context);
            case LEFT_CLICK_ENTITY_ATTACK -> onLeftClickEntityAttack(context);
            case RIGHT_CLICK_AIR -> onRightClickAir(context);
            case RIGHT_CLICK_BLOCK -> onRightClickBlock(context);
            case RIGHT_CLICK_ENTITY_USE -> onRightClickEntityUse(context);
        };
    }

    default WandExecutionResult onLeftClickAir(WandContext context) {
        return onUnhandledInteraction(context);
    }

    default WandExecutionResult onLeftClickBlock(WandContext context) {
        return onUnhandledInteraction(context);
    }

    default WandExecutionResult onLeftClickEntityAttack(WandContext context) {
        return onUnhandledInteraction(context);
    }

    default WandExecutionResult onRightClickAir(WandContext context) {
        return onUnhandledInteraction(context);
    }

    default WandExecutionResult onRightClickBlock(WandContext context) {
        return onUnhandledInteraction(context);
    }

    default WandExecutionResult onRightClickEntityUse(WandContext context) {
        return onUnhandledInteraction(context);
    }

    default WandExecutionResult onUnhandledInteraction(WandContext context) {
        return WandExecutionResult.passThrough();
    }
}
