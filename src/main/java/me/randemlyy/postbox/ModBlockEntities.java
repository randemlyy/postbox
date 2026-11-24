package me.randemlyy.postbox;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {

    public static final ResourceKey<BlockEntityType<?>> MAILBOX_KEY =
            ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, Postbox.id("mailbox"));

    public static final BlockEntityType<MailboxBlockEntity> MAILBOX = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            MAILBOX_KEY,
            new BlockEntityType<>(MailboxBlockEntity::new, Set.of(ModBlocks.MAILBOX))
    );

    private ModBlockEntities() {}

    public static void init() {}
}