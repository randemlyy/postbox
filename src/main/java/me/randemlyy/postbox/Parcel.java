package me.randemlyy.postbox;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

public record Parcel(
        UUID senderId,
        String senderName,
        UUID recipientId,
        String recipientName,
        ItemStack stack,
        long sentAt
) {

    public static final Codec<Parcel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("sender").forGetter(Parcel::senderId),
            Codec.STRING.fieldOf("sender_name").forGetter(Parcel::senderName),
            UUIDUtil.CODEC.fieldOf("recipient").forGetter(Parcel::recipientId),
            Codec.STRING.fieldOf("recipient_name").forGetter(Parcel::recipientName),
            ItemStack.CODEC.fieldOf("stack").forGetter(Parcel::stack),
            Codec.LONG.fieldOf("sent_at").forGetter(Parcel::sentAt)
    ).apply(instance, Parcel::new));
}