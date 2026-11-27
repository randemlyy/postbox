package me.randemlyy.postbox.net;

import me.randemlyy.postbox.Postbox;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public record SealLetterResultPayload(boolean success, String key, String arg) implements CustomPacketPayload {

    public static final Type<SealLetterResultPayload> TYPE = new Type<>(Postbox.id("seal_letter_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SealLetterResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, SealLetterResultPayload::success,
                    ByteBufCodecs.stringUtf8(64), SealLetterResultPayload::key,
                    ByteBufCodecs.stringUtf8(64), SealLetterResultPayload::arg,
                    SealLetterResultPayload::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}