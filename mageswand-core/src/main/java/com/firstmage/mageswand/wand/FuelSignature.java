package com.firstmage.mageswand.wand;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public record FuelSignature(Material material, String metaNbt) {
    public static FuelSignature fromItem(ItemStack stack) {
        ItemMeta itemMeta = stack.getItemMeta();
        String meta = "";
        if (itemMeta != null) {
            String asString = itemMeta.getAsString();
            if (asString != null && !asString.isBlank()) {
                meta = asString.trim();
            }
        }
        return new FuelSignature(stack.getType(), meta);
    }

    public static @Nullable FuelSignature parseConfigKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        String[] parts = key.split("\\|\\|", 2);
        Material material = Material.matchMaterial(parts[0].trim().toUpperCase(Locale.ROOT));
        if (material == null) {
            return null;
        }

        String meta = parts.length == 2 ? parts[1].trim() : "";
        return new FuelSignature(material, meta);
    }
}
