package com.firstmage.mageswand.wand;

public record RegisteredFuel(
        FuelSignature signature,
        String spellName,
        String spellDescription,
        boolean operatorOnly,
        String executorClass,
        String executorSource,
        WandExecutor executor
) {
}
