package com.firstmage.mageswand.wand;

import org.bukkit.NamespacedKey;

public final class WandConstants {
    public static final String FIRST_MAGE_NAME = "FirstMage";
    public static final String WAND_ENTITY_RANGE_MODIFIER_ID = "1775776187935";
    public static final String WAND_BLOCK_RANGE_MODIFIER_ID = "1775776187936";

    public static final double NO_FUEL_DAMAGE = 2.0D;
    public static final String NO_FUEL_MESSAGE = "The wand feels like it is trying to draw power, but there is not the appropriate source of magic...";
    public static final String OP_ONLY_SPELL_MESSAGE = "That spell is limited to operators.";

    public static final int SPELL_PACK_API_VERSION = 1;

    public static final String COMMAND_PERMISSION = "mageswand.command";
    public static final String OP_ONLY_SPELL_PERMISSION = "mageswand.spell.oponly";

    private WandConstants() {
    }

    public static boolean matchesRawModifierId(NamespacedKey key, String rawId) {
        String fullKey = key.getNamespace() + ":" + key.getKey();
        return key.getKey().equals(rawId) || fullKey.equals(rawId) || fullKey.equals("minecraft:" + rawId);
    }
}
