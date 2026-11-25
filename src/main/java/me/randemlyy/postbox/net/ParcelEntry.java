package me.randemlyy.postbox.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record ParcelEntry(String senderName, ItemStack stack) {

    public static final StreamCodec<RegistryFriendlyByteBuf, ParcelEntry> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ParcelEntry::senderName,
                    ItemStack.STREAM_CODEC, ParcelEntry::stack,
                    ParcelEntry::new);

    public ParcelEntry {
        stack = stack.copy();
    }
}
