package me.randemlyy.postbox;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItems {

    public static final ResourceKey<Item> LETTER_KEY =
            ResourceKey.create(Registries.ITEM, Postbox.id("letter"));

    public static final Item LETTER = Registry.register(
            BuiltInRegistries.ITEM,
            LETTER_KEY,
            new Item(new Item.Properties().setId(LETTER_KEY))
    );

    public static void init() {

    }

}
