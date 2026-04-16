package com.firstmage.mageswand.wand;

import org.bukkit.inventory.ItemRarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.List;

public final class WandItems {
    private static final String[] NAME_COLORS = {
            "#d6e7ff", "#cbe3fc", "#c0dffa", "#b5dbf7",
            "#aad7f5", "#9fd3f2", "#94cff0", "#89cbed",
            "#7ec7eb", "#73c3e8", "#68bfe6", "#5dbbe3",
            "#52b7e1", "#47b3de", "#3cafdc", "#31abd9"
    };

    private WandItems() {
    }

    public static boolean isFirstMageWand(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }

        ItemMeta itemMeta = stack.getItemMeta();
        if (itemMeta == null) {
            return false;
        }

        var modifiers = itemMeta.getAttributeModifiers();
        if (modifiers == null || modifiers.isEmpty()) {
            return false;
        }

        for (Collection<AttributeModifier> collection : modifiers.asMap().values()) {
            for (AttributeModifier modifier : collection) {
                NamespacedKey key = modifier.getKey();
                if (WandConstants.matchesRawModifierId(key, WandConstants.WAND_ENTITY_RANGE_MODIFIER_ID)
                        || WandConstants.matchesRawModifierId(key, WandConstants.WAND_BLOCK_RANGE_MODIFIER_ID)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static ItemStack createFirstMageWand() {
        ItemStack stack = new ItemStack(Material.STICK);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(buildGradientName("FirstMage's Wand"));
        meta.lore(List.of(Component.text("What happens when magic is turned against its creator...?")));
        meta.setRarity(ItemRarity.EPIC);
        meta.setEnchantmentGlintOverride(true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 3, true);
        meta.addAttributeModifier(
                Attribute.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(
                        new NamespacedKey("minecraft", WandConstants.WAND_ENTITY_RANGE_MODIFIER_ID),
                        64.0D,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );
        meta.addAttributeModifier(
                Attribute.BLOCK_INTERACTION_RANGE,
                new AttributeModifier(
                        new NamespacedKey("minecraft", WandConstants.WAND_BLOCK_RANGE_MODIFIER_ID),
                        64.0D,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(meta);
        return stack;
    }

    private static Component buildGradientName(String text) {
        Component result = Component.empty();
        for (int index = 0; index < text.length() && index < NAME_COLORS.length; index++) {
            result = result.append(Component.text(String.valueOf(text.charAt(index)))
                    .color(TextColor.fromHexString(NAME_COLORS[index]))
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }
        return result;
    }
}
