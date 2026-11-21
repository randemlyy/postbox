package me.randemlyy.postbox;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

public record LetterContents(String to, String from, String message) {

    public static final String TO_PREFIX = "TO: ";
    public static final String FROM_PREFIX = "FROM: ";

    public static LetterContents from(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);

        // Checking for the FROM, TO and message.
        if (lore == null || lore.lines().size() < 3) {
            return null;
        }

        List<Component> lines = lore.lines();
        String to = lines.get(0).getString();
        String from = lines.get(1).getString();

        if (!to.startsWith(TO_PREFIX) || !from.startsWith(FROM_PREFIX)) {
            return null;
        }

        return new LetterContents(
                to.substring(TO_PREFIX.length()),
                from.substring(FROM_PREFIX.length()),
                lines.get(2).getString()
        );

    }
}