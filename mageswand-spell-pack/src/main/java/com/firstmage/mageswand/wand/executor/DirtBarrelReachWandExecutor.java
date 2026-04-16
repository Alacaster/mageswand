package com.firstmage.mageswand.wand.executor;

import com.firstmage.mageswand.wand.WandContext;
import com.firstmage.mageswand.wand.WandExecutionResult;
import com.firstmage.mageswand.wand.WandExecutor;
import org.bukkit.Material;
import org.bukkit.block.Block;

public final class DirtBarrelReachWandExecutor implements WandExecutor {
    private static final WandExecutionResult EXTENDED_BARREL_BREAK = WandExecutionResult.of(1, true, true, false, false)
            .withAllowExtendedVanillaBlockInteractions(true);

    private static final WandExecutionResult EXTENDED_BARREL_INTERACT = WandExecutionResult.of(1, true, false, false, false)
            .withAllowExtendedVanillaBlockInteractions(true);

    @Override
    public WandExecutionResult onLeftClickBlock(WandContext context) {
        return isBarrel(context.targetBlock())
                ? EXTENDED_BARREL_BREAK
                : WandExecutionResult.passThrough();
    }

    @Override
    public WandExecutionResult onRightClickBlock(WandContext context) {
        return isBarrel(context.targetBlock())
                ? EXTENDED_BARREL_INTERACT
                : WandExecutionResult.passThrough();
    }

    private boolean isBarrel(Block block) {
        return block != null && block.getType() == Material.BARREL;
    }
}
