package me.randemlyy.postbox;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;

public class ModItems {
    public static final ResourceKey<Item> LETTER_KEY =
            ResourceKey.create(Registries.ITEM, Postbox.id("letter"));

    public static final Item LETTER = Registry.register(
            BuiltInRegistries.ITEM,
            LETTER_KEY,
            new LetterItem(new Item.Properties().setId(LETTER_KEY).stacksTo(1))
    );

    public static final ResourceKey<Item> MAILBOX_KEY =
            ResourceKey.create(Registries.ITEM, Postbox.id("mailbox"));

    public static final Item MAILBOX = Registry.register(
            BuiltInRegistries.ITEM,
            MAILBOX_KEY,
            new BlockItem(ModBlocks.MAILBOX, new Item.Properties().setId(MAILBOX_KEY).useBlockDescriptionPrefix())
    );

    private static final ResourceKey<CreativeModeTab> FUNCTIONAL_BLOCKS = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("functional_blocks"));

    public static void init() {
        CreativeModeTabEvents.modifyOutputEvent(FUNCTIONAL_BLOCKS).register(output -> output.accept(MAILBOX));
    }
}
