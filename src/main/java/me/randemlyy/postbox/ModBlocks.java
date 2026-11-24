package me.randemlyy.postbox;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {

    public static final ResourceKey<Block> MAILBOX_KEY = ResourceKey.create(Registries.BLOCK, Postbox.id("mailbox"));

    public static final MailboxBlock MAILBOX = Registry.register(
            BuiltInRegistries.BLOCK,
            MAILBOX_KEY,
            new MailboxBlock(BlockBehaviour.Properties.of().setId(MAILBOX_KEY).mapColor(MapColor.WOOD).strength(2.0f, 3.0f).sound(SoundType.WOOD))
    );

    private ModBlocks() {}

    public static void init() {}
}
