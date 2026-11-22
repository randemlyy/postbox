package me.randemlyy.postbox;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

public record HeldMail(UUID senderID, String senderName, UUID recipientID, String recipientName, ItemStack stack, long arriveAt, long expireAt) {

    public static final Codec<HeldMail> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("sender").forGetter(HeldMail::senderID),
            Codec.STRING.fieldOf("sender_name").forGetter(HeldMail::senderName),
            UUIDUtil.CODEC.fieldOf("recipient").forGetter(HeldMail::recipientID),
            Codec.STRING.fieldOf("recipient_name").forGetter(HeldMail::recipientName),
            ItemStack.CODEC.fieldOf("stack").forGetter(HeldMail::stack),
            Codec.LONG.fieldOf("arrive_at").forGetter(HeldMail::arriveAt),
            Codec.LONG.fieldOf("expire_at").forGetter(HeldMail::expireAt)
    ).apply(instance, HeldMail::new));

}
