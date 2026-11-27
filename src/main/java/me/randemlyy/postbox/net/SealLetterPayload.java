package me.randemlyy.postbox.net;

import me.randemlyy.postbox.Postbox;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public record SealLetterPayload(boolean mainHand, String to, String message) implements CustomPacketPayload {

    public static final int MAX_NAME = 32;
    public static final int MAX_MESSAGE = 400;

    public static final Type<SealLetterPayload> TYPE = new Type<>(Postbox.id("seal_letter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SealLetterPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, SealLetterPayload::mainHand,
                    ByteBufCodecs.stringUtf8(MAX_NAME), SealLetterPayload::to,
                    ByteBufCodecs.stringUtf8(MAX_MESSAGE), SealLetterPayload::message,
                    SealLetterPayload::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}