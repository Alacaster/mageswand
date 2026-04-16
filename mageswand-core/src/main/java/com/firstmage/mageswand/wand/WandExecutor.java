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
        return WandExecutionResult.passThrough();
    }

    default WandExecutionResult onLeftClickBlock(WandContext context) {
        return WandExecutionResult.passThrough();
    }

    default WandExecutionResult onLeftClickEntityAttack(WandContext context) {
        return WandExecutionResult.passThrough();
    }

    default WandExecutionResult onRightClickAir(WandContext context) {
        return WandExecutionResult.passThrough();
    }

    default WandExecutionResult onRightClickBlock(WandContext context) {
        return WandExecutionResult.passThrough();
    }

    default WandExecutionResult onRightClickEntityUse(WandContext context) {
        return WandExecutionResult.passThrough();
    }
}
