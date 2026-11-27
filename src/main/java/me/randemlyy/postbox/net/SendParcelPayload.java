package me.randemlyy.postbox.net;

import me.randemlyy.postbox.Postbox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public record SendParcelPayload(BlockPos pos, String recipientName, int amount) implements CustomPacketPayload {

    public static final int MAX_NAME_LENGTH = 32;

    public static final Type<SendParcelPayload> TYPE = new Type<>(Postbox.id("send_parcel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SendParcelPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SendParcelPayload::pos,
                    ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH), SendParcelPayload::recipientName,
                    ByteBufCodecs.VAR_INT, SendParcelPayload::amount,
                    SendParcelPayload::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
